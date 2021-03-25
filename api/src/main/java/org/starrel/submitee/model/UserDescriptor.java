package org.starrel.submitee.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserDescriptor {
    String realmType;
    String userId;

    public String toString() {
        return realmType + ":" + userId;
    }
}
