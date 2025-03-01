package co.teamsphere.api.controller;

import co.teamsphere.api.DTO.MessageDTO;
import co.teamsphere.api.DTOmapper.MessageDTOMapper;
import co.teamsphere.api.exception.ChatException;
import co.teamsphere.api.exception.MessageException;
import co.teamsphere.api.models.Messages;
import co.teamsphere.api.models.User;
import co.teamsphere.api.request.SendMessageRequest;
import co.teamsphere.api.response.ApiResponses;
import co.teamsphere.api.services.MessageService;
import co.teamsphere.api.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/message")
@Slf4j
public class MessageController {
    private final MessageDTOMapper messageDTOMapper;
    private final UserService userService;
    private final MessageService messageService;
    public MessageController(UserService userService,
                             MessageService messageService,
                             MessageDTOMapper messageDTOMapper) {
        this.userService = userService;
        this.messageService = messageService;
        this.messageDTOMapper = messageDTOMapper;
    }

    @Operation(summary = "Send a message", description = "Sends a message to a user or group chat.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @ApiResponse(
                        responseCode = "200",
                        description = "Message sent successfully",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(
                                        implementation = MessageDTO.class
                                )
                        )
                ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/create")
    public ResponseEntity<MessageDTO> sendMessageHandler(@RequestHeader("Authorization")String jwt, @RequestBody SendMessageRequest req) throws ChatException {

        try {
            log.info("Processing send message request to userId: {}", req.getUserId());

            User reqUser = userService.findUserProfile(jwt);
            req.setUserId(reqUser.getId());

            Messages messages = messageService.sendMessage(req);

            MessageDTO messageDto = messageDTOMapper.toMessageDto(messages);

            log.info("Message sent successfully by userId: {}", reqUser.getId());

            return new ResponseEntity<>(messageDto, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error during send message process", e);
            throw new ChatException("Error during send message process" + e);
        }
    }

    @Operation(summary = "Get messages for a chat", description = "Retrieves all messages for a specific chat.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @ApiResponse(
                responseCode = "202",
                description = "Messages retrieved successfully",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(
                                implementation = List.class
                        )
                )
                ),
        @ApiResponse(responseCode = "404", description = "Chat not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<MessageDTO>> getChatsMessageHandler(@PathVariable UUID chatId) throws ChatException {
        try {
            log.info("Processing get messages for chat with ID: {}", chatId);

            List<Messages> messages = messageService.getChatsMessages(chatId);

            List<MessageDTO> messageDtos = messageDTOMapper.toMessageDtos(messages);

            log.info("Retrieved {} messages for chat with ID: {}", messageDtos.size(), chatId);

            return new ResponseEntity<>(messageDtos, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            log.error("Error during get messages process for chat with ID: {}", chatId, e);
            throw new ChatException("Error during get messages process" + e);
        }
    }

    @Operation(summary = "Delete a message", description = "Deletes a specific message by its unique ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @ApiResponse(
                        responseCode = "202",
                        description = "Message deleted successfully",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(
                                        implementation = ApiResponses.class
                                )
                        )
                ),
        @ApiResponse(responseCode = "404", description = "Message not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponses> deleteMessageHandler(@PathVariable UUID messageId) throws MessageException {
        try {
            log.info("Processing delete message request for message with ID: {}", messageId);

            messageService.deleteMessage(messageId);

            log.info("Message with ID {} deleted successfully", messageId);

            ApiResponses res = new ApiResponses("Message deleted", true);

            return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            log.error("Error during delete message process for message with ID: {}", messageId, e);
            throw new MessageException("Error during delete message process" + e);
        }
    }

}
