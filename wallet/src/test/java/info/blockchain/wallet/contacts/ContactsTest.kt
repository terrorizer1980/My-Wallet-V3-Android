package info.blockchain.wallet.contacts

import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.FrameworkInterface
import info.blockchain.wallet.MockInterceptor
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.api.PersistentUrls
import info.blockchain.wallet.bip44.HDWallet
import info.blockchain.wallet.bip44.HDWalletFactory
import info.blockchain.wallet.bip44.HDWalletFactory.Language
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.PaymentBroadcasted
import info.blockchain.wallet.contacts.data.PaymentCurrency
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest
import info.blockchain.wallet.exceptions.MetadataException
import info.blockchain.wallet.exceptions.SharedMetadataException
import info.blockchain.wallet.metadata.data.Message
import info.blockchain.wallet.util.MetadataUtil
import info.blockchain.wallet.util.RestClient

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.spongycastle.crypto.InvalidCipherTextException

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class ContactsTest {

    internal var mockInterceptor = MockInterceptor()

    private val magic =
        "{\"payload\":\"VSSU6yr2Y63SD/q9uzEK5YrXo8n+i/Li6RreS53oFupMuyuNIQB2tJ0Ek1tGMW" +
            "bB+zzZDR6E4NjXWHoZ6tXfxYdlNvs/SSg4y83Xm2P5UQi3zQ+UlwCC75d46UpoBQh9QHh56j9" +
            "VvBXKfkkm9m1fHBGeevG8dM3FxOziHXwalaShv01F1w8Q7BHN8m9KLkg3ELajhpbAqX6V6OeH" +
            "fH2/OqqW9BMVURCWn1a0IF8O32se08kU9y3saOOXx/QBEHKGxP7GwpftnUgT28BkwEjB6Q6A+" +
            "AYuwnJxoa36GqVSNMw2gv10Gxjic59L2FfJvjg40oXjhhnnGfyQtCWFqj15GX15Kv0Krn/oLL" +
            "ZM0gERe0fpelRVYG2iK2+ytYh76s0L\",\"version\":1,\"type_id\":4,\"signature\"" +
            ":\"IIhKFiFFlQRsNcJsk3Pa45CtwnmBCxRCq7ncxScXK/U6XawV3zza7RvGyAp3M41cdXYOvmF" +
            "FErQAp0TZytJQ+qo=\",\"prev_magic_hash\":\"e00c9cfe5756507508a07fddd5139491" +
            "f1a52a0e087593627ae0490297a48842\",\"address\":\"1LF1QvtK6gnxJ3f8tZx9hamWS" +
            "9jytKZJ6C\",\"created_at\":1482153702000,\"updated_at\":1502285812000}"

    private val success = "{\"status\":\"success\"}"
    private val fail = "{\"status\":\"fail\"}"

    private val wallet: HDWallet
        @Throws(Exception::class)
        get() = HDWalletFactory
            .restoreWallet(
                PersistentUrls.getInstance().bitcoinParams, Language.US,
                "15e23aa73d25994f1921a1256f93f72c", "", 1
            )

    @Before
    fun setup() {
        // Set environment
        BlockchainFramework.init(object : FrameworkInterface {

            override val apiCode: String
                get() = ""

            override fun getRetrofitApiInstance(): Retrofit {

                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(mockInterceptor) // Mock responses
                    .addInterceptor(loggingInterceptor) // Extensive logging
                    .build()
                return RestClient.getRetrofitApiInstance(okHttpClient)
            }

            override fun getRetrofitExplorerInstance(): Retrofit? {
                return null
            }

            override fun getEnvironment(): Environment {
                return Environment.PRODUCTION
            }

            override fun getBitcoinParams(): NetworkParameters {
                return BitcoinMainNetParams.get()
            }

            override fun getBitcoinCashParams(): NetworkParameters {
                return BitcoinCashMainNetParams.get()
            }

            override fun getDevice(): String? {
                return null
            }

            override fun getAppVersion(): String? {
                return null
            }
        })
    }

    private fun init(): Contacts {
        val b_wallet = wallet
        val sharedMetaDataHDNode = MetadataUtil.deriveSharedMetadataNode(b_wallet.masterKey)
        val metaDataHDNode = MetadataUtil.deriveMetadataNode(b_wallet.masterKey)

        mockInterceptor.setResponseString(magic)
        return Contacts(metaDataHDNode, sharedMetaDataHDNode)
    }

    @Test
    fun fetch() {
        val contacts = init()
        mockInterceptor.setResponseString(magic)
        contacts.fetch()
        Assert.assertEquals(contacts.getContactList().size.toLong(), 2)
    }

    @Test
    fun fetch_IOException() {
        val contacts = init()
        try {
            mockInterceptor.setIOException(true)
            contacts.fetch()
        } catch (e: MetadataException) {
            e.printStackTrace()
        } catch (e: IOException) {
            Assert.assertTrue(true)
            return
        } catch (e: InvalidCipherTextException) {
            e.printStackTrace()
        } finally {
            mockInterceptor.setIOException(false)
        }
        Assert.assertTrue("IOException not caught", false)
    }

    @Test
    fun save() {
        val contacts = init()
        val contact = Contact()
        contact.name = "John"

        val responseList = LinkedList<String>()
        responseList.add(success) // add contact response
        responseList.add(success) // save response
        mockInterceptor.setResponseStringList(responseList)

        contacts.addContact(contact)
        contacts.save()

        Assert.assertTrue(true)
    }

    @Test
    fun save_IOException() {
        val contacts = init()
        val contact = Contact()
        contact.name = "John"

        mockInterceptor.setResponseString(success)
        contacts.addContact(contact)

        mockInterceptor.setIOException(true)
        try {
            contacts.save()
        } catch (e: IOException) {
            Assert.assertTrue(true)
            return
        } catch (e: MetadataException) {
            e.printStackTrace()
        } catch (e: InvalidCipherTextException) {
            e.printStackTrace()
        } finally {
            mockInterceptor.setIOException(false)
        }
        Assert.assertTrue(false)
    }

    @Test
    fun wipe() {
        val contacts = init()
        val contact = Contact()
        contact.name = "John"

        val responseList = LinkedList<String>()
        responseList.add(success)
        responseList.add(success)
        mockInterceptor.setResponseStringList(responseList)

        contacts.addContact(contact)
        contacts.wipe()

        Assert.assertTrue(contacts.getContactList().size == 0)
    }

    @Test
    fun wipe_IOException() {
        val contacts = init()
        val contact = Contact()
        contact.name = "John"

        mockInterceptor.setResponseString(success)
        contacts.addContact(contact)

        mockInterceptor.setIOException(true)
        try {
            contacts.wipe()
        } catch (e: IOException) {
            Assert.assertTrue(true)
            return
        } catch (e: MetadataException) {
            e.printStackTrace()
        } catch (e: InvalidCipherTextException) {
            e.printStackTrace()
        } finally {
            mockInterceptor.setIOException(false)
        }
        Assert.assertTrue(false)
    }

    @Test
    fun getMdid() {
        val contacts = init()
        Assert.assertEquals("1borrXJLeFgwF1aKS3io9c3rQ1uXHf1s5", contacts.mdid)
    }

    @Test
    fun setContactList() {

        val contacts = init()

        val responseList = LinkedList<String>()
        responseList.add(success)
        responseList.add(success)
        responseList.add(success)
        mockInterceptor.setResponseStringList(responseList)

        contacts.addContact(Contact())
        contacts.addContact(Contact())

        Assert.assertEquals(2, contacts.getContactList().size.toLong())

        contacts.setContactList(ArrayList())

        Assert.assertEquals(0, contacts.getContactList().size.toLong())
    }

    @Test
    fun setContactList_IOException() {

        val contacts = init()

        val responseList = LinkedList<String>()
        responseList.add(success)
        responseList.add(success)
        responseList.add(fail)
        mockInterceptor.setResponseStringList(responseList)

        contacts.addContact(Contact())
        contacts.addContact(Contact())

        Assert.assertEquals(2, contacts.getContactList().size.toLong())

        mockInterceptor.setIOException(true)
        try {
            contacts.setContactList(ArrayList())
        } catch (e: MetadataException) {
            Assert.assertTrue(false)
        } catch (e: IOException) {
            Assert.assertTrue(true)
        } catch (e: InvalidCipherTextException) {
            Assert.assertTrue(false)
        } finally {
            mockInterceptor.setIOException(false)
        }

        Assert.assertEquals(0, contacts.getContactList().size.toLong())
    }

    @Test
    fun addContact() {

        val contacts = init()

        val responseList = LinkedList<String>()
        responseList.add(success)
        responseList.add(success)
        mockInterceptor.setResponseStringList(responseList)

        contacts.addContact(Contact())

        Assert.assertEquals(1, contacts.getContactList().size.toLong())

        contacts.addContact(Contact())
        Assert.assertEquals(2, contacts.getContactList().size.toLong())
    }

    @Test
    fun removeContact() {

        val c1 = Contact()
        val c2 = Contact()

        val contacts = init()

        val responseList = LinkedList<String>()
        responseList.add(success)
        responseList.add(success)
        responseList.add(success)
        mockInterceptor.setResponseStringList(responseList)

        contacts.addContact(c1)
        contacts.addContact(c2)

        Assert.assertEquals(2, contacts.getContactList().size.toLong())

        contacts.removeContact(c1)
        Assert.assertEquals(1, contacts.getContactList().size.toLong())
    }

    @Test
    fun publishXpub() {
        val contacts = init()

        val responseList = LinkedList<String>()
        responseList.add(fail) // magic - string doesn't matter 404 will be caught
        responseList.add(success) // put metadata
        mockInterceptor.setResponseStringList(responseList)

        val responseCodeList = LinkedList<Int>()
        responseCodeList.add(404) // fetch magic - 404 = new magic hash
        responseCodeList.add(200)
        mockInterceptor.setResponseCodeList(responseCodeList)

        try {
            contacts.publishXpub()
            Assert.assertTrue(true)
        } catch (e: MetadataException) {
            e.printStackTrace()
            Assert.fail()
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail()
        } catch (e: InvalidCipherTextException) {
            e.printStackTrace()
            Assert.fail()
        }
    }

    @Test
    fun publishXpub_IOException() {
        val contacts = init()

        try {
            mockInterceptor.setIOException(true)
            contacts.publishXpub()
        } catch (e: MetadataException) {
            e.printStackTrace()
        } catch (e: IOException) {
            Assert.assertTrue(true)
            return
        } catch (e: InvalidCipherTextException) {
            e.printStackTrace()
        } finally {
            mockInterceptor.setIOException(false)
        }
        Assert.fail()
    }

    @Test
    fun fetchXpub() {
        val contacts = init()
        mockInterceptor.setResponseString(
            "{\"payload\":\"eyJ4cHViIjoieHB1YjY4aGpMM01rdmZ6S1pSdXZmQUFBZWFiYUF" +
                "uWmpnWXFVM0ZTbTFMRUNxRGNhVHI1N013YzREY2lHcTJKQnJyVG9zOHNuUHg3OG" +
                "1MdEt4dGFQSzJMcWJoVTVnMW9QRk1hR29uRTI3a0g4S0dBIn0=\",\"version\":" +
                "1,\"type_id\":4,\"signature\":\"IE2zczTK0sRPLRu/vfbM3v6S7gAIh2o+U" +
                "Qxkn1P4uUT+KqCU+P8kVEt7SLjixIQqSb4UzlKNKirXUBiNWGU4Ygg=\",\"prev_magic" +
                "_hash\":\"8f122f88cad5faedcc3433dbf0618cea17a5682da2c3dfdf36d03d63f88a" +
                "90c6\",\"created_at\":1482242589000,\"updated_at\":1482244810000,\"addr" +
                "ess\":\"16uJDcPbvegnJUhgXr5TW9nd9wbJYNWBAd\"}")

        val xpub = contacts.fetchXpub("16uJDcPbvegnJUhgXr5TW9nd9wbJYNWBAd")

        Assert.assertTrue(xpub == "xpub68hjL3MkvfzKZRuvfAAAeabaAnZjgYqU3FSm1LECqDcaTr57Mw" +
            "c4DciGq2JBrrTos8snPx78mLtKxtaPK2LqbhU5g1oPFMaGonE27kH8KGA")
    }

    @Test
    fun fetchXpub_IOException() {
        val contacts = init()
        mockInterceptor.setResponseString(
            "{\"payload\":\"eyJ4cHViIjoieHB1YjY4aGpMM01rdmZ6S1pSdXZmQUFBZWFiYUFuWmpnWXFVM0" +
                "ZTbTFMRUNxRGNhVHI1N013YzREY2lHcTJKQnJyVG9zOHNuUHg3OG1MdEt4dGFQSzJMcWJoVTVn" +
                "MW9QRk1hR29uRTI3a0g4S0dBIn0=\",\"version\":1,\"type_id\":4,\"signature\":\"I" +
                "E2zczTK0sRPLRu/vfbM3v6S7gAIh2o+UQxkn1P4uUT+KqCU+P8kVEt7SLjixIQqSb4UzlKNKirXU" +
                "BiNWGU4Ygg=\",\"prev_magic_hash\":\"8f122f88cad5faedcc3433dbf0618cea17a5682d" +
                "a2c3dfdf36d03d63f88a90c6\",\"created_at\":1482242589000,\"updated_at\":148224" +
                "4810000,\"address\":\"16uJDcPbvegnJUhgXr5TW9nd9wbJYNWBAd\"}")
        try {
            mockInterceptor.setIOException(true)
            contacts.fetchXpub("16uJDcPbvegnJUhgXr5TW9nd9wbJYNWBAd")
        } catch (e: MetadataException) {
            e.printStackTrace()
        } catch (e: IOException) {
            Assert.assertTrue(true)
            return
        } catch (e: InvalidCipherTextException) {
            e.printStackTrace()
        } finally {
            mockInterceptor.setIOException(false)
        }
        Assert.assertTrue(false)
    }

    @Test
    fun createInvitation() {
        // Not testable in current state
    }

    @Test
    fun createInvitation_IOException() {
        val contacts = init()
        val me = Contact()
        me.name = "Me"
        val him = Contact()
        him.name = "Him"

        var myInvite: Contact? = null
        try {
            mockInterceptor.setIOException(true)
            myInvite = contacts.createInvitation(me, him)
        } catch (e: IOException) {
            Assert.assertTrue(true)
            return
        } catch (e: SharedMetadataException) {
            e.printStackTrace()
        } finally {
            mockInterceptor.setIOException(false)
        }

        Assert.assertTrue(contacts.getContactList().filterValues { it.name == "Him" }.isNotEmpty())
        Assert.assertTrue(myInvite!!.name == "Me")
        Assert.assertTrue(myInvite.mdid == "1borrXJLeFgwF1aKS3io9c3rQ1uXHf1s5")
    }

    @Test
    fun readInvitationLink() {
        val contacts = init()
        val receivedInvite =
            contacts.readInvitationLink(
                "http://blockchain.info/invite?id=852bb6796c2aefb7ea9" +
                    "6131b785da397dca9cb3bee5df4ea7c937493613e9c37&name=Me")

        Assert.assertTrue(receivedInvite.name == "Me")
        Assert.assertTrue(receivedInvite.invitationReceived ==
            "852bb6796c2aefb7ea96131b785da397dca9cb3bee5df4ea7c937493613e9c37")
    }

    @Test
    fun acceptInvitationLink() {
        // Not testable in current state
    }

    @Test
    fun digestUnreadPaymentRequests_RPR() {

        val contacts = init()

        val contact = Contact()
        contact.id = "5b47394c-f0d1-416e-8e9d-d63a91709d03"
        contact.name = "Jacob"
        contact.mdid = "13cA57Hvs5zT8yq852aUZeoYfX9DBXCTTR"
        contact.facilitatedTransactions = HashMap()
        mockInterceptor.setResponseString(success)
        contacts.addContact(contact)

        val rpr = RequestForPaymentRequest()
        rpr.id = "a9feb110-1ae2-4242-9246-f1d6ec3e3be8"
        rpr.intendedAmount = 17940000
        rpr.currency = PaymentCurrency.BITCOIN
        rpr.note = "For the pizza"

        val messages = ArrayList<Message>()
        val message = Message()
        message.payload = rpr.toJson()
        message.signature =
            "IDNOxhoWL/gj12kNGte37e2oxzK/A9lVeH3YyjDg" +
                "k4TTRO0YlkYQnHxS0qcvY8EnVMHhELUgzv/7IQrkypCuktU="

        message.recipient = "17uHsZXWqXB5ChW5fNGPnxyTJQEd1ugKca"
        message.id = "cc11e4cf-2cd4-4de5-a2fc-125c43625ec5"
        message.sender = "13cA57Hvs5zT8yq852aUZeoYfX9DBXCTTR"
        message.sent = 1485788921000L
        message.isProcessed = false
        message.type = 0
        messages.add(message)

        mockInterceptor.setResponseString(success)
        val unreadPaymentRequests =
            contacts.digestUnreadPaymentRequests(messages, false)

        for (item in unreadPaymentRequests) {

            val ftx = item.facilitatedTransactions[rpr.id]

            Assert.assertEquals(contact.name, item.name)
            Assert.assertEquals(rpr.id, ftx?.getId())
            Assert.assertEquals("waiting_address", ftx?.state)
            Assert.assertEquals(17940000L, ftx?.intendedAmount)
            Assert.assertEquals(rpr.currency, PaymentCurrency.BITCOIN)
            Assert.assertEquals("rpr_receiver", ftx?.role)
            Assert.assertEquals(rpr.note, ftx?.note)
        }
    }

    @Test
    fun digestUnreadPaymentRequests_RP() {

        val contacts = init()

        val contact = Contact()
        contact.id = "5b47394c-f0d1-416e-8e9d-d63a91709d03"
        contact.name = "Jacob"
        contact.mdid = "13cA57Hvs5zT8yq852aUZeoYfX9DBXCTTR"
        contact.facilitatedTransactions = HashMap()
        mockInterceptor.setResponseString(success)
        contacts.addContact(contact)

        /* Set up Payment Request */
        val pr = PaymentRequest()
        pr.id = "a9feb110-1ae2-4242-9246-f1d6ec3e3be8"
        pr.intendedAmount = 28940000
        pr.currency = PaymentCurrency.BITCOIN
        pr.note = "For the pizza"
        pr.address = "15sAyHb9zBsZbVnaSXz2UivTZYxnjjrEkX"

        var messages: MutableList<Message> = ArrayList()
        var message = Message()
        message.payload = pr.toJson()
        message.signature =
            "IDNOxhoWL/gj12kNGte37e2oxzK/A9lVeH3YyjDgk4" +
                "TTRO0YlkYQnHxS0qcvY8EnVMHhELUgzv/7IQrkypCuktU="

        message.recipient = "17uHsZXWqXB5ChW5fNGPnxyTJQEd1ugKca"
        message.id = "cc11e4cf-2cd4-4de5-a2fc-125c43625ec5"
        message.sender = "13cA57Hvs5zT8yq852aUZeoYfX9DBXCTTR"
        message.sent = 1485788921000L
        message.isProcessed = false
        message.type = 1
        messages.add(message)

        mockInterceptor.setResponseString(success) // save
        var unreadPaymentRequests = contacts.digestUnreadPaymentRequests(messages, false)
        for (item in unreadPaymentRequests) {

            val ftx = item.facilitatedTransactions[pr.id]

            Assert.assertEquals(contact.name, item.name)
            Assert.assertEquals(pr.id, ftx?.id)
            Assert.assertEquals("waiting_payment", ftx?.state)
            Assert.assertEquals(28940000L, ftx?.intendedAmount)
            Assert.assertEquals("pr_receiver", ftx?.role)
            Assert.assertEquals(pr.note, ftx?.note)
            Assert.assertEquals(pr.address, ftx?.address)
        }

        /* Complete above payment request */
        val b = PaymentBroadcasted(pr.id, "this_will_be_the_tx_hash")

        messages = ArrayList()
        message = Message()
        message.payload = b.toJson()
        message.signature =
            "IDNOxhoWL/gj12kNGte37e2oxzK/A9lVeH3YyjDgk4T" +
                "TRO0YlkYQnHxS0qcvY8EnVMHhELUgzv/7IQrkypCuktU="

        message.recipient = "17uHsZXWqXB5ChW5fNGPnxyTJQEd1ugKca"
        message.id = "cc11e4cf-2cd4-4de5-a2fc-125c43625ec5"
        message.sender = "13cA57Hvs5zT8yq852aUZeoYfX9DBXCTTR"
        message.sent = 1485788921000L
        message.isProcessed = false
        message.type = 2
        messages.add(message)

        mockInterceptor.setResponseString(success)
        unreadPaymentRequests = contacts.digestUnreadPaymentRequests(messages, false)
        for (item in unreadPaymentRequests) {

            val ftx = item.facilitatedTransactions[b.id]

            Assert.assertEquals(contact.name, item.name)
            Assert.assertEquals(b.id, ftx?.getId())
            Assert.assertEquals("payment_broadcasted", ftx?.getState())
            Assert.assertEquals(28940000L, ftx?.getIntendedAmount())
            Assert.assertEquals("pr_receiver", ftx?.getRole())
            Assert.assertEquals(pr.note, ftx?.getNote())
            Assert.assertEquals(pr.address, ftx?.getAddress())
        }
    }
}