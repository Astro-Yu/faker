package nextstep;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import nextstep.generator.MockDataGenerator;

public class Main {
    public static void main(String[] args) {
        try {
            // CSV 파일이 저장될 경로 (프로젝트 루트의 'mock-data' 폴더)
            String outputPath = "mock-data";
            Files.createDirectories(Paths.get(outputPath)); // 디렉토리 생성

            MockDataGenerator generator = new MockDataGenerator(outputPath);
            generator.customGenerate(100, 20000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
