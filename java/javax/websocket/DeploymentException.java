package javax.websocket;

/**
 * 在部署WebSocket端点过程中发生问题时抛出的异常。
 *
 * @author Joe Walnes
 * @author Danniel Pfeifer
 */
public class DeploymentException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * 使用指定的错误消息构造一个新的 DeploymentException。
     *
     * @param message 错误消息
     */
    public DeploymentException(String message) {
        super(message);
    }

    /**
     * 使用指定的错误消息和原因构造一个新的 DeploymentException。
     *
     * @param message 错误消息
     * @param cause   导致此异常的根本原因
     */
    public DeploymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
