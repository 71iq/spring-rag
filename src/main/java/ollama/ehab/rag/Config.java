package ollama.ehab.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class Config {

    private final VectorStore vectorStore;
    private final Resource pdfResource;

    public Config(VectorStore vectorStore,
                  @Value("classpath:/Ehab Maali-CV.pdf") Resource pdfResource) {
        this.vectorStore = vectorStore;
        this.pdfResource = pdfResource;
    }

    @PostConstruct
    public void init() {
        PagePdfDocumentReader documentReader = new PagePdfDocumentReader(pdfResource);
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        vectorStore.add(tokenTextSplitter.apply(documentReader.get()));
    }
}
