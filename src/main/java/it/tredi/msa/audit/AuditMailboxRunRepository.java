package it.tredi.msa.audit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditMailboxRunRepository extends MongoRepository<AuditMailboxRun, String> {

	public AuditMailboxRun findByMailboxName(String mailboxName);
	
}

