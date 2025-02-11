package com.jyhun.LiveStream.repository;

import com.jyhun.LiveStream.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
