package info.blockchain.wallet.contacts

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.contacts.data.PaymentBroadcasted
import info.blockchain.wallet.contacts.data.PaymentCancelledResponse
import info.blockchain.wallet.contacts.data.PaymentDeclinedResponse
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.contacts.data.PublicContactDetails
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest
import info.blockchain.wallet.exceptions.MetadataException
import info.blockchain.wallet.exceptions.SharedMetadataException
import info.blockchain.wallet.metadata.Metadata
import info.blockchain.wallet.metadata.SharedMetadata
import info.blockchain.wallet.metadata.data.Invitation
import info.blockchain.wallet.metadata.data.Message
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.util.ArrayList
import java.util.HashMap
import java.util.NoSuchElementException
import org.bitcoinj.crypto.DeterministicKey
import org.slf4j.LoggerFactory
import org.spongycastle.crypto.InvalidCipherTextException
import org.spongycastle.util.encoders.Base64

@Suppress("unused")
class Contacts constructor(
    metaDataHDNode: DeterministicKey,
    sharedMetaDataHDNode: DeterministicKey
) {

    private val metadata: Metadata = Metadata.Builder(metaDataHDNode, METADATA_TYPE_EXTERNAL).build()
    private val sharedMetadata: SharedMetadata = SharedMetadata.Builder(sharedMetaDataHDNode).build()
    private var contactList: HashMap<String, Contact> = HashMap()
    private val mapper = ObjectMapper()

    /**
     * Returns your shared metadata mdid
     */
    val mdid: String
        get() = sharedMetadata.address

    /**
     * Retrieves contact list from metadata service
     */
    fun fetch() {
        log.info("Fetching contact list")
        val data = metadata.metadata
        contactList = if (data != null) {
            mapper.readValue<HashMap<String, Contact>>(
                data,
                object : TypeReference<Map<String, Contact>>() {}
            )
        } else {
            HashMap()
        }

        log.info("Fetching contact list. Size = {}", contactList.size)
    }

    /**
     * Saves contact list to metadata service
     */
    fun save() {
        log.info("Saving contact list")
        metadata.putMetadata(mapper.writeValueAsString(contactList))
    }

    /**
     * Wipes contact list on metadata service as well as local contact list
     */
    fun wipe() {
        log.info("Wiping contact list")
        metadata.putMetadata(mapper.writeValueAsString(HashMap<Any, Any>()))
        contactList = HashMap()
    }

    /**
     * Invalidates auth token
     */
    fun invalidateToken() = sharedMetadata.setToken(null)

    fun getContactList(): HashMap<String, Contact> = contactList

    /**
     * Overwrites contact list.
     */
    fun setContactList(contacts: List<Contact>) {
        contactList.clear()
        contacts.forEach { contactList[it.id] = it }

        save()
    }

    /**
     * Adds contact to contact list.
     */
    fun addContact(contact: Contact) {
        log.info("Adding contact {}", contact.id)
        contactList[contact.id] = contact
        save()
    }

    /**
     * Removes contact from contact list.
     */
    fun removeContact(contact: Contact) {
        log.info("Removing contact {}", contact.id)
        contactList.remove(contact.id)
        if (contact.mdid != null) {
            sharedMetadata.deleteTrusted(contact.mdid)
        }
        save()
    }

    /**
     * Removes contact from contact list using mdid.
     */
    fun removeContact(mdid: String) {
        val contact = getContactFromMdid(mdid)

        log.info("Removing contact {}", contact!!.id)
        contactList.remove(contact.id)
        sharedMetadata.deleteTrusted(contact.mdid)
        save()
    }

    /**
     * Renames a [Contact] based on their ID. Saves changes to server.
     *
     * @param contactId The Contact's ID (Note: not MDID)
     * @param newName The new name for the Contact
     */
    fun renameContact(contactId: String, newName: String) {
        getContactList()[contactId]?.let {
            it.name = newName
            save()
        } ?: throw NullPointerException("Contact not found")
    }

    /**
     * Deletes a [FacilitatedTransaction] from a [Contact] and saves to the server.
     * You'll want to sync the contacts list after failure if an exception is propagated.
     *
     * @param mdid A [Contact.mdid]
     * @param fctxId A [FacilitatedTransaction.getId]
     */
    fun deleteFacilitatedTransaction(mdid: String, fctxId: String) {
        log.info("Deleting facilitated transaction {}", fctxId)
        getContactFromMdid(mdid)?.let {
            it.deleteFacilitatedTransaction(fctxId)
            save()
        } ?: throw NullPointerException("Contact not found")
    }

    /**
     * Publishes your mdid-xpub pair unencrypted to metadata service
     */
    fun publishXpub() {
        log.info("Publishing mdid-xpub pair")
        val details = PublicContactDetails(sharedMetadata.xpub)

        Metadata().apply {
            setEncrypted(false)
            address = sharedMetadata.address
            node = sharedMetadata.node
            setType(METADATA_TYPE_EXTERNAL)
            fetchMagic()
            putMetadata(details.toJson())
        }
    }

    /**
     * Fetches unencrypted xpub associated with mdid
     */
    fun fetchXpub(mdid: String): String? {
        log.info("Fetching mdid's xpub")
        val data = metadata.getMetadata(mdid, false)

        if (data != null) {
            val (xpub) = PublicContactDetails().fromJson(data)
            return xpub
        } else {
            throw MetadataException("Xpub not found")
        }
    }

    /**
     * Creates an invitation [Contact]
     */
    fun createInvitation(myDetails: Contact, recipientDetails: Contact): Contact {
        log.info("Creating inter-wallet-comms invitation")
        myDetails.mdid = sharedMetadata.address

        val i = sharedMetadata.createInvitation()

        myDetails.invitationSent = i.id

        val received = Invitation()
        received.id = i.id

        recipientDetails.invitationSent = i.id

        addContact(recipientDetails)

        return myDetails
    }

    /**
     * Parses invitation uri to [Contact]
     */
    fun readInvitationLink(link: String): Contact {
        log.info("Reading inter-wallet-comms invitation link")
        val queryParams = getQueryParams(link)
        // link will contain contact info, but not mdid
        return Contact().fromQueryParameters(queryParams)
    }

    /**
     * Accepts invitation link and returns [Contact].
     */
    fun acceptInvitationLink(link: String): Contact {
        log.info("Accepting inter-wallet-comms invitation link")
        val queryParams = getQueryParams(link)

        val id = queryParams["id"]

        val accepted: Invitation? =
        try {
            sharedMetadata.acceptInvitation(id)
        } catch (e: SharedMetadataException) {
            // Invitation doesn't exist
            throw NoSuchElementException("Invitation already accepted")
        }

        val contact = Contact().fromQueryParameters(queryParams)
        contact.mdid = accepted!!.mdid
        contact.invitationReceived = accepted.id

        publishXpub()
        sharedMetadata.addTrusted(accepted.mdid)
        addContact(contact)
        contact.xpub = fetchXpub(accepted.mdid)
        save()

        return contact
    }

    /**
     * Checks if sent invitation has been accepted. If accepted, the invitee is added to contact
     * list.
     */
    fun readInvitationSent(invite: Contact): Boolean {
        var accepted = false

        val recipientMdid = sharedMetadata.readInvitation(invite.invitationSent)

        if (recipientMdid != null) {

            val contact = getContactFromSentInviteId(invite.invitationSent!!)
            contact!!.mdid = recipientMdid

            sharedMetadata.addTrusted(recipientMdid)
            addContact(contact)
            contact.xpub = fetchXpub(recipientMdid)

            // Contact accepted invite, we can update and delete invite now
            sharedMetadata.deleteInvitation(invite.invitationSent)

            accepted = true

            save()
        }
        log.info("Checking if invitation has been accepted - {}", accepted)

        return accepted
    }

    private fun getContactFromSentInviteId(id: String): Contact? {
        return contactList.values.firstOrNull { it.invitationSent == id }
    }

    /**
     * Send message
     */
    fun sendMessage(mdid: String, message: String, type: Int, encrypted: Boolean) {
        log.info("Sending inter-wallet-comms message")
        val b64Message: String

        if (encrypted) {
            val recipientXpub = getContactFromMdid(mdid)!!.xpub
                ?: throw SharedMetadataException("No public xpub for mdid.")

            b64Message = sharedMetadata.encryptFor(recipientXpub, message)
        } else {
            b64Message = String(Base64.encode(message.toByteArray(charset("utf-8"))))
        }

        sharedMetadata.postMessage(mdid, b64Message, type)
    }

    /**
     * Retrieves received messages
     */
    fun getMessages(onlyNew: Boolean): List<Message> {
        log.info("Fetching inter-wallet-comms messages")
        val messages = sharedMetadata!!.getMessages(onlyNew)

        val i = messages.iterator()
        while (i.hasNext()) {
            val message = i.next()
            val contactFromMdid = getContactFromMdid(message.sender)
            if (contactFromMdid?.xpub != null) {
                try {
                    decryptMessageFrom(message, contactFromMdid.xpub!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: InvalidCipherTextException) {
                    e.printStackTrace()
                } catch (e: MetadataException) {
                    e.printStackTrace()
                }
            } else {
                // Edge case since Android will not allow contact invitation without a published xpub
                log.warn("Unable to decrypt message - Sender's xpub might not be published")
                markMessageAsRead(message.id, true)
                i.remove()
            }
        }

        return messages
    }

    /**
     * Returns [Message]
     */
    fun readMessage(messageId: String): Message {
        return sharedMetadata.getMessage(messageId)
    }

    /**
     * Flag message as read
     */
    fun markMessageAsRead(messageId: String, markAsRead: Boolean) =
        sharedMetadata.processMessage(messageId, markAsRead)

    private fun decryptMessageFrom(message: Message, xpub: String): Message {
        log.info("Decrypting inter-wallet-comms message")
        return message.apply { payload = sharedMetadata.decryptFrom(xpub, message.payload) }
    }

    private fun getQueryParams(uri: String): Map<String, String> {
        val a = URI.create(uri)

        val params = HashMap<String, String>()

        for (param in a.query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val pair = param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val key = URLDecoder.decode(pair[0], "UTF-8")
            val value = URLDecoder.decode(pair[1], "UTF-8")
            params[key] = value
        }

        return params
    }

    private fun getContactFromMdid(mdid: String): Contact? =
        contactList.values.firstOrNull { it.mdid == mdid }

    /**
     * Send request for payment request. (Ask recipient to send a bitcoin receive address)
     */
    fun sendRequestForPaymentRequest(mdid: String, request: RequestForPaymentRequest) {
        log.info("Sending inter-wallet-comms request for payment request")

        val tx = FacilitatedTransaction().apply {
            intendedAmount = request.intendedAmount
            currency = request.currency
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_INITIATOR
            note = request.note
            updateCompleted()
        }
        request.id = tx.id

        sendMessage(mdid, request.toJson(), TYPE_PAYMENT_REQUEST, true)

        getContactFromMdid(mdid)?.addFacilitatedTransaction(tx)
        save()
    }

    /**
     * Sends new payment request without need to ask for receive address.
     */
    fun sendPaymentRequest(mdid: String, request: PaymentRequest) {

        log.info("Sending inter-wallet-comms payment request")
        val facilitatedTransaction = FacilitatedTransaction().apply {
            request.id = id

            sendMessage(mdid, request.toJson(), TYPE_PAYMENT_REQUEST_RESPONSE, true)

            note = request.note
            intendedAmount = request.intendedAmount
            currency = request.currency
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_PR_INITIATOR
            updateCompleted()
        }

        getContactFromMdid(mdid)?.addFacilitatedTransaction(facilitatedTransaction)
        save()
    }

    /**
     * Send payment request response
     */
    fun sendPaymentRequest(mdid: String, request: PaymentRequest, fTxId: String) {

        log.info("Sending inter-wallet-comms payment request response")
        sendMessage(mdid, request.toJson(), TYPE_PAYMENT_REQUEST_RESPONSE, true)

        getContactFromMdid(mdid)!!.facilitatedTransactions[fTxId]?.let {
            it.state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            it.updateCompleted()
        }
        save()
    }

    /**
     * Sends notification that transaction has been processed.
     */
    fun sendPaymentBroadcasted(mdid: String, txHash: String, fTxId: String) {

        log.info("Sending inter-wallet-comms notification that transaction has been processed.")

        sendMessage(
            mdid,
            PaymentBroadcasted(fTxId, txHash).toJson(),
            TYPE_PAYMENT_BROADCASTED,
            true
        )

        val contact = getContactFromMdid(mdid)
        contact!!.facilitatedTransactions[fTxId]?.let {
            it.state = FacilitatedTransaction.STATE_PAYMENT_BROADCASTED
            it.txHash = txHash
            it.updateCompleted()
        }

        save()
    }

    /**
     * Sends notification that the payment request has been declined.
     */
    fun sendPaymentDeclined(mdid: String, fTxId: String) {

        log.info("Sending inter-wallet-comms notification that transaction has been declined.")
        val contact = getContactFromMdid(mdid)
        val ftx = contact!!.facilitatedTransactions[fTxId]

        sendMessage(
            mdid,
            PaymentDeclinedResponse(fTxId).toJson(),
            TYPE_DECLINE_REQUEST,
            true
        )

        ftx?.let {
            it.state = FacilitatedTransaction.STATE_DECLINED
            it.updateCompleted()
        }

        save()
    }

    /**
     * Sends notification that the payment request has been cancelled.
     */
    fun sendPaymentCancelled(mdid: String, fTxId: String) {

        log.info("Sending inter-wallet-comms notification that transaction has been cancelled.")

        sendMessage(
            mdid,
            PaymentCancelledResponse(fTxId).toJson(),
            TYPE_CANCEL_REQUEST,
            true
        )

        val contact = getContactFromMdid(mdid)
        contact!!.facilitatedTransactions[fTxId]?.let {
            it.state = FacilitatedTransaction.STATE_CANCELLED
            it.updateCompleted()
        }

        save()
    }

    /**
     * Digests unread payment requests and returns a list of [Contact] with [ ] that need responding to.
     */
    fun digestUnreadPaymentRequests(): List<Contact> =
        digestUnreadPaymentRequests(getMessages(true), true)

    internal fun digestUnreadPaymentRequests(
        messages: List<Message>,
        markAsRead: Boolean
    ): List<Contact> {
        val unread = ArrayList<Contact>()

        log.info("Digesting inter-wallet-comms payment requests.")

        for (message in messages) {
            when (message.type) {
                TYPE_PAYMENT_REQUEST -> {
                    val rpr = RequestForPaymentRequest().fromJson(message.payload)

                    val tx = FacilitatedTransaction().apply {
                        id = rpr.id
                        intendedAmount = rpr.intendedAmount
                        currency = rpr.currency
                        state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
                        role = FacilitatedTransaction.ROLE_RPR_RECEIVER
                        note = rpr.note
                        updateCompleted()
                    }

                    val contact = getContactFromMdid(message.sender)!!
                    contact.addFacilitatedTransaction(tx)
                    unread.add(contact)
                    if (markAsRead) markMessageAsRead(message.id, true)
                }
                TYPE_PAYMENT_REQUEST_RESPONSE -> {
                    val pr = PaymentRequest().fromJson(message.payload)
                    val contact = getContactFromMdid(message.sender)
                    var tx = contact!!.facilitatedTransactions[pr.id]

                    var newlyCreated = false
                    if (tx == null) {
                        tx = FacilitatedTransaction().apply {
                            id = pr.id
                            intendedAmount = pr.intendedAmount
                            currency = pr.currency
                            role = FacilitatedTransaction.ROLE_PR_RECEIVER
                            note = pr.note
                        }
                        newlyCreated = true
                    }

                    tx.state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
                    tx.address = pr.address
                    tx.updateCompleted()

                    unread.add(contact)
                    if (markAsRead) markMessageAsRead(message.id, true)
                    if (newlyCreated) {
                        contact.addFacilitatedTransaction(tx)
                    }
                }
                TYPE_PAYMENT_BROADCASTED -> {
                    val pb = PaymentBroadcasted().fromJson(message.payload)
                    val contact = getContactFromMdid(message.sender)

                    contact!!.facilitatedTransactions[pb.id]?.let {
                        it.state = FacilitatedTransaction.STATE_PAYMENT_BROADCASTED
                        it.txHash = pb.txHash
                        it.updateCompleted()
                    }

                    unread.add(contact)
                    if (markAsRead) markMessageAsRead(message.id, true)
                }
                TYPE_DECLINE_REQUEST -> {
                    val declined = PaymentDeclinedResponse().fromJson(message.payload)
                    val contact = getContactFromMdid(message.sender)

                    contact!!.facilitatedTransactions[declined.fctxId]?.let {
                        it.state = FacilitatedTransaction.STATE_DECLINED
                        it.updateCompleted()
                    }
                    unread.add(contact)
                    if (markAsRead) markMessageAsRead(message.id, true)
                }
                TYPE_CANCEL_REQUEST -> {
                    val cancelled = PaymentCancelledResponse().fromJson(message.payload)
                    val contact = getContactFromMdid(message.sender)

                    contact!!.facilitatedTransactions[cancelled.fctxId]?.let {
                        it.state = FacilitatedTransaction.STATE_CANCELLED
                        it.updateCompleted()
                    }

                    unread.add(contact)
                    if (markAsRead) markMessageAsRead(message.id, true)
                }
            }
        }

        if (messages.isNotEmpty()) {
            save()
        }

        return unread
    }

    companion object {

        /**
         * Payment request types
         */
        private const val TYPE_PAYMENT_REQUEST = 0
        private const val TYPE_PAYMENT_REQUEST_RESPONSE = 1
        private const val TYPE_PAYMENT_BROADCASTED = 2
        private const val TYPE_DECLINE_REQUEST = 3
        private const val TYPE_CANCEL_REQUEST = 4

        /**
         * Metadata node type
         */
        private const val METADATA_TYPE_EXTERNAL = 4

        private val log = LoggerFactory.getLogger(Contacts::class.java)
    }
}
