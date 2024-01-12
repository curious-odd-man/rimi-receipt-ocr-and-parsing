import com.github.curiousoddman.receipt.parsing.config.PathsConfig;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.ocr.OcrResult;
import com.github.curiousoddman.receipt.parsing.ocr.OcrServiceProvider;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.TsvParser;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi.RimiText2Receipt;
import com.github.curiousoddman.receipt.parsing.utils.PathsUtils;
import com.github.curiousoddman.receipt.parsing.validation.ItemNumbersValidator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.curiousoddman.receipt.parsing.utils.JsonUtils.OBJECT_WRITER;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        OcrServiceProvider.class,
        RimiText2Receipt.class,
        TsvParser.class,
        ItemNumbersValidator.class,
        PathsUtils.class
})
@Slf4j
public class EndToEndTest {
    private static final Path INPUT_FILE           = Path.of("src/test/resources/e2e.pdf");
    private static final Path EXPECTED_OUTPUT_FILE = Path.of("src/test/resources/e2e.pdf.json");

    @Autowired
    private OcrServiceProvider ocrServiceProvider;
    @Autowired
    private RimiText2Receipt   rimiText2Receipt;
    @MockBean
    PathsConfig pathsConfig;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        Path tempDirectory = Files.createTempDirectory("ReceiptParsing-EndToEndTest");
        log.info("Created temp directory for test data: {}", tempDirectory);
        when(pathsConfig.getCachesRoot()).thenReturn(tempDirectory.toAbsolutePath().toString());
    }

    @Test
    @SneakyThrows
    void test() {
        OpenCV.loadLocally();
        String sourcePdfName = INPUT_FILE.toFile().getName();
        OcrResult ocrResult = ocrServiceProvider.get().getCachedOrDoOcr(INPUT_FILE);
        Receipt receipt = rimiText2Receipt.parse(sourcePdfName, ocrResult, ocrServiceProvider.get());
        String receiptJson = OBJECT_WRITER.writeValueAsString(receipt);
        String expectedJson = Files.readString(EXPECTED_OUTPUT_FILE);
        JSONAssert.assertEquals(expectedJson, receiptJson, true);
    }
}
