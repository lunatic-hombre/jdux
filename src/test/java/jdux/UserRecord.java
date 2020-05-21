package jdux;

import java.time.ZonedDateTime;
import java.util.List;

public record UserRecord(int id, String name, ZonedDateTime lastLogin, List<RoleRecord> roles) {}
