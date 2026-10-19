package com.placetrack.demo;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.placetrack.model.Company;

import jakarta.persistence.EntityManagerFactory;

/**
 * Shows SessionFactory, Session and Transaction by hand: insert a company, roll back, prove it is gone.
 * Runs only with: ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
 */
@Component
@Profile("demo")
@Order(2) // after DataSeeder, so there is data to query
public class HibernateSessionDemo implements CommandLineRunner {

    private final SessionFactory sessionFactory;

    /** Spring Boot builds a JPA EntityManagerFactory; underneath it is Hibernate's SessionFactory. */
    public HibernateSessionDemo(EntityManagerFactory entityManagerFactory) {
        this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    }

    // Runs once at startup (only with the demo profile)
    @Override
    public void run(String... args) {
        System.out.println("\n===== Hibernate demo: SessionFactory -> Session -> Transaction =====");

        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            System.out.println("1. HQL GROUP BY, students per department:");
            List<Object[]> rows = session.createQuery(
                    "select s.department, count(s) from Student s group by s.department", Object[].class).list();
            for (Object[] row : rows) {
                System.out.println("     " + row[0] + " = " + row[1]);
            }

            System.out.println("2. Companies before insert: " + countCompanies(session));
            session.persist(new Company("Rollback Demo Ltd", "Demo"));
            session.flush(); // send the INSERT now, still inside the open transaction
            System.out.println("3. Companies after insert (inside the transaction): " + countCompanies(session));

            tx.rollback();
            System.out.println("4. tx.rollback() called");
        }

        try (Session session = sessionFactory.openSession()) {
            System.out.println("5. Companies seen by a NEW session: " + countCompanies(session)
                    + "  -> the insert was undone");
        }
        System.out.println("=====================================================================\n");
    }

    // HQL count query run inside the given session
    private Long countCompanies(Session session) {
        return session.createQuery("select count(c) from Company c", Long.class).getSingleResult();
    }
}
