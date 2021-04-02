package org.starrel.submitee.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder
@Data
@AllArgsConstructor
public class UserDescriptor {
    @Getter
    String realmType;
    @Getter
    String userId;

    public String toString() {
        return realmType + ":" + userId;
    }
}
