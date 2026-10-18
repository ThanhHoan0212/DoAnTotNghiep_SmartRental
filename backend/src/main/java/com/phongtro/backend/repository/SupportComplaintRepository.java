package com.phongtro.backend.repository;

import com.phongtro.backend.entity.SupportComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;

public interface SupportComplaintRepository extends JpaRepository<SupportComplaint, UUID>, JpaSpecificationExecutor<SupportComplaint> {}
