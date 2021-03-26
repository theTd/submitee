package org.starrel.submitee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult<TReturns> {
    public final static ExecutionResult<Void> SUCCESS_NO_RETURNS = createSuccess(null);
    public final static ExecutionResult<Void> FAIL_NO_MESSAGE = createFail("", "");

    @Builder.Default
    boolean success = false;
    @Builder.Default
    TReturns returns = null;
    @Builder.Default
    String failClassify = "";
    @Builder.Default
    String failMessage = "";

    public static <TReturns> ExecutionResult<TReturns> createSuccess(TReturns returns) {
        //noinspection unchecked
        return (ExecutionResult<TReturns>) ExecutionResult.builder().success(true).returns(returns).build();
    }

    public static <TReturns> ExecutionResult<TReturns> createFail(String failClassify, String failMessage) {
        //noinspection unchecked
        return (ExecutionResult<TReturns>) ExecutionResult.builder().success(false).failClassify(failClassify).failMessage(failMessage).build();
    }
}
