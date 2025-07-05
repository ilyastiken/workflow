package ma.xproce.workflow.repositories;

import ma.xproce.workflow.entities.User;
import ma.xproce.workflow.entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUser(User user);

    @Transactional
    void deleteByUser(User user);
}