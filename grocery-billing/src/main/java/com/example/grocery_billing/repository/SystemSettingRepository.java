package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SystemSettingRepository
        extends JpaRepository<SystemSetting, String> {

    Optional<SystemSetting> findByKey(@org.springframework.data.repository.query.Param("key") String key);
}
