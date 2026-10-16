package contactmanagement.repository;

import contactmanagement.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testSaveAndExistById() {
        User user = new User();
        user.setUsername("Yogaharis");
        user.setPassword(passwordEncoder.encode("rahasia123"));
        user.setName("Yoga Haris");

        userRepository.save(user);

        boolean isExistUser = userRepository.existsById(user.getUsername());

        Assertions.assertTrue(isExistUser);
    }
}