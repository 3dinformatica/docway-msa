package it.tredi.msa.audit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditMessageRepository extends MongoRepository<AuditMessage, String> {

	public AuditMessage findByMessageIdAndMailboxName(String messageId, String mailboxName);
	public AuditMessage findByMessageIdAndMailboxNameAndStatus(String messageId, String mailboxName, AuditMessageStatus status);
	
}

