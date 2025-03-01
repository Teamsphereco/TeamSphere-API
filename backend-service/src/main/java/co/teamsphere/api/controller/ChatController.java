package co.teamsphere.api.controller;

import co.teamsphere.api.DTO.ChatDTO;
import co.teamsphere.api.DTO.ChatSummaryDTO;
import co.teamsphere.api.DTOmapper.ChatDTOMapper;
import co.teamsphere.api.exception.ChatException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.models.Chat;
import co.teamsphere.api.models.User;
import co.teamsphere.api.request.GroupChatRequest;
import co.teamsphere.api.request.RenameGroupChatRequest;
import co.teamsphere.api.request.SingleChatRequest;
import co.teamsphere.api.services.ChatService;
import co.teamsphere.api.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {

    private final ChatService chatService;

    private final UserService userService;
    
    private final ChatDTOMapper chatDTOMapper;

    public ChatController(ChatService chatService,
                          UserService userService,
                          ChatDTOMapper chatDTOMapper
    ) {
        this.chatService = chatService;
        this.userService = userService;
        this.chatDTOMapper = chatDTOMapper;
    }

    @PostMapping("/single")
    @Operation(summary = "Create a single chat",
        description = "Creates a single chat between the authenticated user and another user."
        )
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Chat created successfully",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(
                                implementation = ChatDTO.class
                        )
                )
                ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<ChatDTO> creatChatHandler(@RequestBody SingleChatRequest singleChatRequest,
                                                    @RequestHeader("Authorization")  String jwt) throws UserException {
        log.info("single chat --------");
        User reqUser = userService.findUserProfile(jwt);
        Chat chat = chatService.createChat(reqUser.getId(),singleChatRequest.getUserId(),false);
        ChatDTO chatDto = chatDTOMapper.toChatDto(chat);
        return new ResponseEntity<>(chatDto, HttpStatus.OK);
    }

    @PostMapping("/group")
    @Operation(summary = "Create a group chat", description = "Creates a group chat with the specified users.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Group chat created successfully",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(
                                implementation = ChatDTO.class
                        )
                )
                ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<ChatDTO> createGroupHandler(@RequestBody GroupChatRequest groupChatRequest,
                                                      @RequestHeader("Authorization") String jwt)
            throws UserException {
        User reqUser = userService.findUserProfile(jwt);
        Chat chat = chatService.createGroup(groupChatRequest, reqUser.getId());
        ChatDTO chatDto = chatDTOMapper.toChatDto(chat);
        return new ResponseEntity<>(chatDto, HttpStatus.OK);
    }

    @GetMapping("/{chatId}")
    @Operation(summary = "Get chat by ID", description = "Fetches a chat by its unique ID.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Chat fetched successfully",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(
                                implementation = ChatDTO.class
                        )
                )
                ),
        @ApiResponse(responseCode = "404", description = "Chat not found")
    })
    public ResponseEntity<ChatDTO> findChatByIdHandler(@PathVariable UUID chatId) throws ChatException {
        Chat chat = chatService.findChatById(chatId);
        ChatDTO chatDto = chatDTOMapper.toChatDto(chat);
        return new ResponseEntity<>(chatDto, HttpStatus.OK);
    }

    @PutMapping("/{chatId}/add/{userId}")
    @Operation(summary = "Add user to group chat", description = "Adds a user to an existing group chat.")
    @ApiResponses({
        @ApiResponse(
                    responseCode = "200",
                    description = "User added to group successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ChatDTO.class
                            )
                    )
                ),
        @ApiResponse(responseCode = "404", description = "Chat or user not found"),
        @ApiResponse(responseCode = "403", description = "Unauthorized action")
    })
    public ResponseEntity<ChatDTO> addUserToGroupHandler(@PathVariable UUID chatId,
                                                         @PathVariable UUID userId)
            throws UserException, ChatException {
        Chat chat = chatService.addUserToGroup(userId, chatId);
        ChatDTO chatDto = chatDTOMapper.toChatDto(chat);
        return new ResponseEntity<>(chatDto, HttpStatus.OK);
    }

    @PutMapping("/{chatId}/rename")
    @Operation(summary = "Rename group chat", description = "Renames an existing group chat.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Group renamed successfully",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(
                                implementation = ChatDTO.class
                        )
                )
                ),
        @ApiResponse(responseCode = "404", description = "Chat not found"),
        @ApiResponse(responseCode = "403", description = "Unauthorized action")
    })
    public ResponseEntity<ChatDTO> renameGroupHandler(@PathVariable UUID chatId,
                                                      @RequestBody RenameGroupChatRequest renameGroupRequest,
                                                      @RequestHeader("Authorization") String jwt)
            throws ChatException, UserException {
        User reqUser = userService.findUserProfile(jwt);
        Chat chat = chatService.renameGroup(chatId, renameGroupRequest.getGroupName(), reqUser.getId());
        ChatDTO chatDto = chatDTOMapper.toChatDto(chat);
        return new ResponseEntity<>(chatDto, HttpStatus.OK);
    }

    @PutMapping("/{chatId}/remove/{userId}")
    @Operation(summary = "Remove user from group chat", description = "Removes a user from an existing group chat.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "User removed successfully",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(
                                implementation = ChatDTO.class
                        )
                )
                ),
        @ApiResponse(responseCode = "404", description = "Chat or user not found"),
        @ApiResponse(responseCode = "403", description = "Unauthorized action")
    })
    public ResponseEntity<ChatDTO> removeFromGroupHandler(@RequestHeader("Authorization") String jwt,
                                                          @PathVariable UUID chatId,
                                                          @PathVariable UUID userId)
            throws UserException, ChatException {
        User reqUser=userService.findUserProfile(jwt);
        Chat chat = chatService.removeFromGroup(chatId, userId, reqUser.getId());
        ChatDTO chatDto = chatDTOMapper.toChatDto(chat);
        return new ResponseEntity<>(chatDto, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{chatId}/{userId}")
    @Operation(summary = "Delete chat", description = "Deletes a chat based on its ID and the user's ID.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Chat deleted successfully",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(
                                implementation = ChatDTO.class
                        )
                )
                ),
        @ApiResponse(responseCode = "404", description = "Chat not found"),
        @ApiResponse(responseCode = "403", description = "Unauthorized action")
    })
    public ResponseEntity<ChatDTO> deleteChatHandler(@PathVariable UUID chatId,
                                                     @PathVariable UUID userId) throws ChatException, UserException{
        Chat chat = chatService.deleteChat(chatId, userId);
        ChatDTO chatDto = chatDTOMapper.toChatDto(chat);
        return new ResponseEntity<>(chatDto, HttpStatus.OK);
    }

    @GetMapping("/summaries")
    @Operation(summary = "Get chat summaries", description = "Fetches summaries of chats for the authenticated user.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Chat summaries retrieved successfully",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(
                                implementation = List.class
                        )
                )
                ),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ChatSummaryDTO>> getChatSummariesHandler(
            @RequestHeader("Authorization") String jwt,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) throws ChatException {
        try {
            log.info("Fetching chat summaries for user");
            User user = userService.findUserProfile(jwt);
            // Fetch chat summaries with pagination
            List<ChatSummaryDTO> chatSummaries = chatService.getChatSummaries(user.getId(), page, size);
            log.info("Retrieved {} chat summaries for user ID: {}", chatSummaries.size(), user.getId());
            return new ResponseEntity<>(chatSummaries, HttpStatus.OK);
        } catch (ChatException e) {
            log.error("User error fetching chat summaries: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error fetching chat summaries", e);
            throw new ChatException("Error fetching chat summaries: " + e.getMessage());
        }
    }
}
