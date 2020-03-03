package info.blockchain.wallet.metadata;

import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.exceptions.MetadataException;
import info.blockchain.wallet.metadata.data.RemoteMetadataNodes;
import info.blockchain.wallet.util.MetadataUtil;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.DeterministicKey;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
/**
 * Restores derived metadata nodes from a metadata node derived from user credentials.
 * This is to avoid repeatedly asking user for second password.
 */
public class MetadataNodeFactory {

    private DeterministicKey sharedMetadataNode;
    private DeterministicKey metadataNode;

    private Metadata secondPwNode;
    private Metadata secondPwNodeLegacy;

    public MetadataNodeFactory(String guid, String sharedKey, String walletPassword) throws Exception {
        this.secondPwNode = deriveSecondPasswordNode(guid, sharedKey, walletPassword);
        this.secondPwNodeLegacy = deriveSecondPasswordNodeLegacy(guid, sharedKey, walletPassword);
    }

    public void deletePwNodeLegacy() {
        deleteMetadataFromNode(secondPwNodeLegacy);
    }

    public void fetchMagicFowSecondPwNode() throws IOException, MetadataException {
        secondPwNode.fetchMagic();
    }

    public void fetchMagicFowSecondPwNodeLegacy() throws IOException, MetadataException {
        secondPwNodeLegacy.fetchMagic();
    }


    public boolean isMetadataUsable() {
        try {
            String nodesJson = secondPwNode.getMetadata();
            if (nodesJson == null) {
                //no record
                //prompt for second pw if need then saveMetadataHdNodes()
                return false;
            } else {
                //Yes load nodes from stored metadata.
                return loadNodes(RemoteMetadataNodes.fromJson(nodesJson));
            }

        } catch (Exception e) {
            //Metadata decryption will fail if user changes wallet password.
            e.printStackTrace();
            return false;
        }
    }

    private boolean loadNodes(RemoteMetadataNodes remoteMetadataNodes) {
        //If not all nodes available fail.
        if (!remoteMetadataNodes.isAllNodesAvailable()) {
            return false;
        }

        sharedMetadataNode = DeterministicKey.deserializeB58(
                remoteMetadataNodes.getMdid(),
                PersistentUrls.getInstance().getBitcoinParams());
        metadataNode = DeterministicKey.deserializeB58(
                remoteMetadataNodes.getMetadata(),
                PersistentUrls.getInstance().getBitcoinParams());

        return true;
    }

    public boolean saveMetadataHdNodes(DeterministicKey masterKey) throws Exception {
        //Derive nodes
        DeterministicKey md = MetadataUtil.INSTANCE.deriveMetadataNode(masterKey);
        DeterministicKey smd = MetadataUtil.INSTANCE.deriveSharedMetadataNode(masterKey);

        //Save nodes hex on 2nd pw metadata
        RemoteMetadataNodes remoteMetadataNodes = new RemoteMetadataNodes();
        remoteMetadataNodes.setMdid(smd.serializePrivB58(PersistentUrls.getInstance().getBitcoinParams()));
        remoteMetadataNodes.setMetadata(md.serializePrivB58(PersistentUrls.getInstance().getBitcoinParams()));
        secondPwNode.putMetadata(remoteMetadataNodes.toJson());

        return loadNodes(remoteMetadataNodes);
    }

    private Metadata deriveSecondPasswordNodeLegacy(String guid, String sharedkey, String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String text = guid + sharedkey;
        md.update(text.getBytes(StandardCharsets.UTF_8));
        byte[] entropy = md.digest();
        BigInteger bi = new BigInteger(1, entropy);

        ECKey key = ECKey.fromPrivate(bi);

        byte[] enc = AESUtil.stringToKey(password + sharedkey, 5000);

        final String address = key.toAddress(PersistentUrls.getInstance().getBitcoinParams()).toString();
        return new Metadata(address, key, enc, true, -1);  // TODO -1? WTF
    }

    private Metadata deriveSecondPasswordNode(String guid, String sharedkey, String password) throws Exception {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String input = guid + sharedkey + password;
        md.update(input.getBytes(StandardCharsets.UTF_8));
        byte[] entropy = md.digest();
        BigInteger bi = new BigInteger(1, entropy);

        ECKey key = ECKey.fromPrivate(bi);

        final String address = key.toAddress(PersistentUrls.getInstance().getBitcoinParams()).toString();
        return new Metadata(address, key, key.getPrivKeyBytes(), true, -1);
    }

    boolean isLegacySecondPwNodeAvailable() {
        try {
            return secondPwNodeLegacy.getMetadata() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void deleteMetadataFromNode(Metadata node) {
        try {
            String nodesJson = node.getMetadata();
            if (nodesJson != null) {
                RemoteMetadataNodes remoteMetadataNodes = RemoteMetadataNodes.fromJson(nodesJson);
                node.deleteMetadata(remoteMetadataNodes.toJson());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DeterministicKey getSharedMetadataNode() {
        return sharedMetadataNode;
    }

    public DeterministicKey getMetadataNode() {
        return metadataNode;
    }
}
