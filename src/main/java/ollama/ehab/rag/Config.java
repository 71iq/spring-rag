package ollama.ehab.rag;

import jakarta.annotation.PostConstruct;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class Config {

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;

    public Config(VectorStore vectorStore, ResourceLoader resourceLoader) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        Resource resource = resourceLoader.getResource("classpath:data/");
        try {
            Path dataPath = Paths.get(resource.getURI());
            try (Stream<Path> paths = Files.walk(dataPath)) {
                paths.filter(Files::isRegularFile)
                        .forEach(file -> {
                            if (file.getFileName().toString().toLowerCase().endsWith("pdf")) {
                                processPdf(file);
                                System.out.println("PDF" + file);
                            }
                            if (file.getFileName().toString().toLowerCase().endsWith("csv")) {
                                processCsv(file);
                                System.out.println("CSV" + file);
                            }
                            if (file.getFileName().toString().toLowerCase().endsWith("docx")) {
                                processDocx(file);
                                System.out.println("DOCX" + file);
                            }
                        });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processCsv(Path csvPath) {
        try {
            Resource csvResource = resourceLoader.getResource("classpath:data/" + csvPath.getFileName().toString());
            BufferedReader reader = new BufferedReader(new InputStreamReader(csvResource.getInputStream()));
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().build());
            var rows = new ArrayList<Document>();
            rows.add(new Document(csvPath.getFileName().toString()));
            for (CSVRecord record : csvParser) {
                StringBuilder con = new StringBuilder();
                for (String title : csvParser.getHeaderNames())
                    if (!(record.get(title).isEmpty() || record.get(title).isBlank()))
                        con.append(title).append(": ").append(record.get(title)).append(", ");
                rows.add(new Document(csvPath.getFileName().toString().split("\\.")[0] + " on date: " + record.get(1) + " - " + con));
            }
            System.out.println(rows.stream().map(Document::getContent).collect(Collectors.joining("\nNEW ROW ")));
            csvParser.close();
            reader.close();
            TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
            vectorStore.add(tokenTextSplitter.apply(rows));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processDocx(Path docxPath) {
        try (FileInputStream fis = new FileInputStream(docxPath.toFile())) {
            XWPFDocument docx = new XWPFDocument(fis);
            String fileName = docxPath.getFileName().toString().split("\\.")[0];
            var doc = new ArrayList<Document>();
            for (XWPFParagraph paragraph : docx.getParagraphs())
                doc.add(new Document(fileName + " " + paragraph.getText()));
            System.out.println(doc.stream().map(Document::getContent).collect(Collectors.joining("\nNEW ROW ")));
            TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
            vectorStore.add(tokenTextSplitter.apply(doc));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processPdf(Path pdfPath) {
        try {
            Resource pdfResource = resourceLoader.getResource("classpath:data/" + pdfPath.getFileName().toString());
            PagePdfDocumentReader documentReader = new PagePdfDocumentReader(pdfResource);
            String fileName = pdfPath.getFileName().toString().split("\\.")[0];
            List<Document> documents = documentReader.get();

            List<Document> modifiedDocuments = documents.stream()
                    .map(document -> new Document(fileName + " " + document.getContent()))
                    .collect(Collectors.toList());

            System.out.println(modifiedDocuments.stream().map(Document::getContent).collect(Collectors.joining("\nNEW ROW ")));

            TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
            vectorStore.add(tokenTextSplitter.apply(modifiedDocuments));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
