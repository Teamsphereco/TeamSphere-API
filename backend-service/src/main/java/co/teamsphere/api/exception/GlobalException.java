package co.teamsphere.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalException {
    // TODO: think out and add more errors (too lazy atm)
    @ExceptionHandler(ProfileImageException.class)
    public ResponseEntity<ErrorDetail> ProfileImageExceptionHandler(ProfileImageException profileImageException, WebRequest req){
        var error = new ErrorDetail(profileImageException.getMessage(), req.getDescription(false), LocalDateTime.now().atOffset(ZoneOffset.UTC));

        return new ResponseEntity<>(error, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }


    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorDetail> UserExceptionHandler(UserException userException, WebRequest req){
        ErrorDetail error = new ErrorDetail(userException.getMessage(), req.getDescription(false), LocalDateTime.now().atOffset(ZoneOffset.UTC));

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MessageException.class)
    public ResponseEntity<ErrorDetail> MessageExceptionHandler(MessageException messageException,WebRequest req){

        ErrorDetail error = new ErrorDetail(messageException.getMessage(), req.getDescription(false), LocalDateTime.now().atOffset(ZoneOffset.UTC));

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetail> MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException methodArgumentNotValidException){
        String error = Objects.requireNonNull(methodArgumentNotValidException.getBindingResult().getFieldError()).getDefaultMessage();

        ErrorDetail err =new ErrorDetail("Validation Error", error ,LocalDateTime.now().atOffset(ZoneOffset.UTC));

        return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ChatException.class)
    public  ResponseEntity<ErrorDetail> ChatExceptionHandler(ChatException chatException, WebRequest req) {
        // Grabbing the original message from the exception and preparing a message to return to user
        String originalMessage = chatException.getMessage();
        String messageToReturn = "";

        if (originalMessage == null) {
            messageToReturn = "A chat error has occurred, but no details were provided.";
        } else if (Pattern.compile("(?i).*chat not exist.*").matcher(originalMessage).find()) {
            messageToReturn = "The chat you are looking for does not exist.";
        } else if (Pattern.compile("(?i).*finding chat by ID.*").matcher(originalMessage).find()) {
            messageToReturn = "Unable to locate the specified chat.";
        } else if (Pattern.compile("(?i).*permission.*").matcher(originalMessage).find()) {
            messageToReturn = "You don't have permission to perform this chat action.";
        } else if (Pattern.compile("(?i).*send message.*").matcher(originalMessage).find()) {
            messageToReturn = "An error occurred while sending the message.";
        } else if (Pattern.compile("(?i).*get messages.*").matcher(originalMessage).find()) {
            messageToReturn = "An error occurred while retrieving chat messages.";
        } else if (Pattern.compile("(?i).*deleting chat.*").matcher(originalMessage).find()) {
            messageToReturn = "An error occurred while deleting the chat.";
        } else if (Pattern.compile("(?i).*retrieving messages.*").matcher(originalMessage).find()) {
            messageToReturn = "Failed to retrieve messages from the chat.";
        } else {
            messageToReturn = "Unexpected chat error has occurred";
        }

        ErrorDetail error = new ErrorDetail(messageToReturn, req.getDescription(false), LocalDateTime.now().atOffset(ZoneOffset.UTC));
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorDetail> handleNoHandlerFoundException(NoHandlerFoundException noHandlerFoundException) {
        ErrorDetail error = new ErrorDetail("Endpoint not found", noHandlerFoundException.getMessage(), LocalDateTime.now().atOffset(ZoneOffset.UTC));
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }



    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetail> otherErrorHandler(Exception e, WebRequest req){

        ErrorDetail error = new ErrorDetail(e.getMessage(), req.getDescription(false), LocalDateTime.now().atOffset(ZoneOffset.UTC));

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
