package org.kickmyb.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kickmyb.server.account.MUser;
import org.kickmyb.server.account.MUserRepository;
import org.kickmyb.server.exceptions.TaskNotFoundException;
import org.kickmyb.server.task.ServiceTask;
import org.kickmyb.transfer.AddTaskRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO pour celui ci on aimerait pouvoir mocker l'utilisateur pour ne pas avoir à le créer

// https://reflectoring.io/spring-boot-mock/#:~:text=This%20is%20easily%20done%20by,our%20controller%20can%20use%20it.

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = KickMyBServerApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@ActiveProfiles("test")
class ServiceTaskTests {

    @Autowired
    private MUserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ServiceTask serviceTask;

    @Test
    void testAddTask() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de test";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        serviceTask.addOne(atr, u);

        assertEquals(1, serviceTask.home(u.id).size());
    }

    @Test
    void testAddTaskEmpty()  {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Empty");
        } catch (Exception e) {
            assertEquals(ServiceTask.Empty.class, e.getClass());
        }
    }

    @Test
    void testAddTaskTooShort() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "o";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.TooShort");
        } catch (Exception e) {
            assertEquals(ServiceTask.TooShort.class, e.getClass());
        }
    }

    @Test
    void testAddTaskExisting() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Bonne tâche";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Existing");
        } catch (Exception e) {
            assertEquals(ServiceTask.Existing.class, e.getClass());
        }
    }
    @Test
    void testDeleteTaskWithCorrectID() throws Exception {
        // Créer un utilisateur
        MUser user = new MUser();
        user.username = "Alice";
        user.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(user);

        // Ajouter une tâche à l'utilisateur
        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche à supprimer";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));
        serviceTask.addOne(atr, user);

        // Récupérer l'utilisateur après ajout de la tâche
        MUser updatedUser = userRepository.findById(user.id).orElseThrow();

        // Vérifier que la tâche a été ajoutée
        assertEquals(1, serviceTask.home(updatedUser.id).size());

        // Supprimer la tâche
        Long taskId = serviceTask.home(updatedUser.id).get(0).id;
        serviceTask.delete(taskId, updatedUser);

        // Vérifier que la tâche n'est plus là
        assertEquals(0, serviceTask.home(updatedUser.id).size());
    }

    @Test
    void testDeleteTaskWithIncorrectID() {
        // Créer un utilisateur
        MUser user = new MUser();
        user.username = "Alice";
        user.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(user);

        // Essayer de supprimer une tâche avec un ID incorrect
        try {
            serviceTask.delete(999L, user);
            fail("Aurait dû lancer TaskNotFoundException");
        } catch (Exception e) {
            assertEquals(TaskNotFoundException.class, e.getClass());
        }
    }

    @Test
    void testAccessControlForTaskDeletion() throws Exception {
        // Créer Alice
        MUser alice = new MUser();
        alice.username = "Alice";
        alice.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(alice);

        // Ajouter une tâche à Alice
        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche d'Alice";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));
        serviceTask.addOne(atr, alice);

        // Vérifier que la tâche a été ajoutée
        MUser updatedAlice = userRepository.findById(alice.id).orElseThrow();
        assertEquals(1, serviceTask.home(updatedAlice.id).size());

        // Créer Bob
        MUser bob = new MUser();
        bob.username = "Bob";
        bob.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(bob);

        // Essayer de supprimer la tâche d'Alice avec Bob
        Long taskId = serviceTask.home(updatedAlice.id).get(0).id;
        try {
            serviceTask.delete(taskId, bob);
            fail("Aurait dû lancer TaskNotFoundException ou une exception de contrôle d'accès");
        } catch (Exception e) {
            assertEquals(TaskNotFoundException.class, e.getClass());
        }

        // Vérifier que la tâche d'Alice est toujours là
        assertEquals(1, serviceTask.home(updatedAlice.id).size());
    }
}
