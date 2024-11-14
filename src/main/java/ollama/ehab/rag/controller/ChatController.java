package ollama.ehab.rag.controller;

import ollama.ehab.rag.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final String chatId;
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
        this.chatId = chatService.establishChat();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "give me summary of all employees") String message) {
        System.out.println("MESSAGE: "  + message);
        String ret =
        chatService.chat(chatId, message)
                .getResults()
                .stream()
                .map(result -> result.getOutput().getContent())
                .reduce((content1, content2) -> content1 + ", " + content2)
                .orElse("");
        System.out.println(ret);
        return ret;
    }
}
