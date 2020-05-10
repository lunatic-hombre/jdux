package jrecordson;

import java.util.List;

public record UserRecord(int id, String name, List<RoleRecord> roles) {}
