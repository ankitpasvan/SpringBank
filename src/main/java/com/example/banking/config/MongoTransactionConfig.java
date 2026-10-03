package com.example.banking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Enables Spring's @Transactional support backed by MongoDB's own multi-document
 * transactions. Needed by TransferService, which must debit one account and credit
 * another as a single all-or-nothing unit - something a single atomic $inc (used by
 * deposit/withdraw) cannot do, since two different documents are involved.
 *
 * IMPORTANT - MongoDB multi-document transactions require a REPLICA SET (or a sharded
 * cluster). They do NOT work against a single standalone mongod. MongoDB Atlas clusters
 * are replica sets by default, so transfers work there without any extra setup. The local
 * fallback URI in application.properties (mongodb://localhost:27017/banking) is typically
 * a standalone instance and will reject transactions unless that local MongoDB is itself
 * started as a (even single-node) replica set.
 */
@Configuration
@EnableTransactionManagement
public class MongoTransactionConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}