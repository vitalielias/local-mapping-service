package edu.kit.datamanager.mappingservice.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.mappingservice.MappingServiceApplication;
import edu.kit.datamanager.mappingservice.dao.IMappingRecordDao;
import edu.kit.datamanager.mappingservice.domain.AclEntry;
import edu.kit.datamanager.mappingservice.domain.MappingRecord;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@EnableRuleMigrationSupport
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = MappingServiceApplication.class)
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"server.port=41500"})
public class MappingExecutionControllerTest {

    private final static String TEMP_DIR_4_ALL = "/tmp/mapping-service/";
    private final static String TEMP_DIR_4_MAPPING = TEMP_DIR_4_ALL + "mapping/";
    private static final String MAPPING_ID = "my_dc";
    private static final String MAPPING_TYPE = "TEST_0.0.0";
    private static final String MAPPING_URL = "/api/v1/mappingExecution/" + MAPPING_ID;
    private static final String MAPPING_TITLE = "TITEL";
    private static final String MAPPING_DESCRIPTION = "DESCRIPTION";

    private MockMvc mockMvc;

    @Autowired
    private IMappingRecordDao mappingRecordDao;

    private void createMapping() throws Exception {
        System.out.println("createMapping");
        File mappingsDir = Paths.get(TEMP_DIR_4_MAPPING).toFile();
        String mappingContent = FileUtils.readFileToString(new File("src/test/resources/mapping/gemma/simple.mapping"), StandardCharsets.UTF_8);
        MappingRecord record = new MappingRecord();
        record.setMappingId(MAPPING_ID);
        record.setMappingType(MAPPING_TYPE);
        record.setTitle(MAPPING_TITLE);
        record.setDescription(MAPPING_DESCRIPTION);
        Set<AclEntry> aclEntries = new HashSet<>();
        aclEntries.add(new AclEntry("SELF", PERMISSION.READ));
        aclEntries.add(new AclEntry("test2", PERMISSION.ADMINISTRATE));
        record.setAcl(aclEntries);
        ObjectMapper mapper = new ObjectMapper();

        MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
        MockMultipartFile mappingFile = new MockMultipartFile("document", "my_dc4gemma.mapping", "application/json", mappingContent.getBytes());

        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/mappingAdministration/").
                file(recordFile).
                file(mappingFile)).
                andDo(print()).
                andExpect(status().isCreated()).
                andExpect(redirectedUrlPattern("http://*:*/api/v1/mappingAdministration/*")).
                andReturn();

        System.out.println(mappingsDir.getAbsolutePath());
    }

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) throws Exception {
        mappingRecordDao.deleteAll();
        try {
            try (Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_ALL)))) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            Paths.get(TEMP_DIR_4_MAPPING).toFile().mkdir();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation)
                        .uris().withPort(8095)
                        .and().operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(Preprocessors.removeHeaders("X-Content-Type-Options", "X-XSS-Protection", "X-Frame-Options"), prettyPrint()))
                .alwaysDo(document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())))
                .build();

        createMapping();
    }

    @Test
    void mapValidDocument() throws Exception {
        String mappingContent = FileUtils.readFileToString(new File("src/test/resources/examples/gemma/simple.json"), StandardCharsets.UTF_8);
//        String resultContent = FileUtils.readFileToString(new File("src/test/resources/result/gemma/simple.elastic.json"), StandardCharsets.UTF_8);
        MockMultipartFile mappingFile = new MockMultipartFile("document", "my_dc4gemma.mapping", "application/json", mappingContent.getBytes());

        this.mockMvc.perform(MockMvcRequestBuilders.multipart(MAPPING_URL).file(mappingFile)).
                andDo(print()).
                andExpect(status().isOk()).
                andExpect(header().string("content-disposition", "attachment;filename=result.txt")).andReturn();
    }

    @Test
    void mapWithoutDocument() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(MAPPING_URL)).
                andDo(print()).
                andExpect(status().isBadRequest()).
                andExpect(status().reason("Required request part 'document' is not present")).
                andReturn();
    }

//    @Test
//    void mapWithInvalidDocument() throws Exception {
//        String mappingContent = FileUtils.readFileToString(new File("src/test/resources/examples/anyContentWithoutSuffix"), StandardCharsets.UTF_8);
//        MockMultipartFile mappingFile = new MockMultipartFile("document", "my_dc4gemma.mapping", "application/json", mappingContent.getBytes());
//
//        this.mockMvc.perform(MockMvcRequestBuilders.multipart(MAPPING_URL).file(mappingFile)).
//                andDo(print()).
//                andExpect(status().isOk()).
//                andExpect(content().string("There is no result for the input. The input must be invalid.")).
//                andReturn();
//    }
    @Test
    void mapWithMissingParameters() throws Exception {
        String mappingContent = FileUtils.readFileToString(new File("src/test/resources/examples/gemma/simple.json"), StandardCharsets.UTF_8);
        MockMultipartFile mappingFile = new MockMultipartFile("document", "my_dc4gemma.mapping", "application/json", mappingContent.getBytes());

        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/mappingExecution/").file(mappingFile)).
                andDo(print()).
                andExpect(status().isNotFound()).
                andReturn();
    }

    @Test
    void mapWithInvalidID() throws Exception {
        String mappingContent = FileUtils.readFileToString(new File("src/test/resources/examples/gemma/simple.json"), StandardCharsets.UTF_8);
        MockMultipartFile mappingFile = new MockMultipartFile("document", "my_dc4gemma.mapping", "application/json", mappingContent.getBytes());

        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/mappingExecution/xsfdfg").file(mappingFile)).
                andDo(print()).
                andExpect(status().isNotFound()).
               //andExpect(content().string("No mapping found for mapping id xsfdfg.")).
                andReturn();
    }

//    @Test
//    void mapWithInvalidType() throws Exception {
//        String mappingContent = FileUtils.readFileToString(new File("src/test/resources/examples/gemma/simple.json"), StandardCharsets.UTF_8);
//        MockMultipartFile mappingFile = new MockMultipartFile("document", "my_dc4gemma.mapping", "application/json", mappingContent.getBytes());
//
//        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/mappingExecution/my_dc/gkahjg").file(mappingFile)).
//                andDo(print()).
//                andExpect(status().isNotFound()).
//                andExpect(content().string("No mapping record found for mapping my_dc/gkahjg.")).
//                andReturn();
//    }
//    @Test
//    void mapWithTypeMissing() throws Exception {
//        String mappingContent = FileUtils.readFileToString(new File("src/test/resources/examples/gemma/simple.json"), StandardCharsets.UTF_8);
//        MockMultipartFile mappingFile = new MockMultipartFile("document", "my_dc4gemma.mapping", "application/json", mappingContent.getBytes());
//
//        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/mappingExecution/my_dc/").file(mappingFile)).
//                andDo(print()).
//                andExpect(status().isNotFound()).
//                andReturn();
//    }
}
