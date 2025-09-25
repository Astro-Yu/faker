package nextstep.generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import net.datafaker.Faker;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class MockDataGenerator {
    private final Faker faker = new Faker(new Locale("ko"));
    private final String outputPath;
    // users와 tags는 다른 데이터 생성 시 필요하므로 메모리에 유지합니다.
    private final List<User> users = new ArrayList<>();
    private final String[] providerTypes = {"GOOGLE", "EMAIL"};
    private final String[] echoTypes = {"SAD", "THANKS", "TOUCHED", "FUNNY", "COMFORTED", "AMAZING"};

    public MockDataGenerator(String outputPath) {
        this.outputPath = outputPath;
    }

    // CSVPrinter를 생성하는 헬퍼 메서드
    private CSVPrinter createCsvPrinter(String fileName, String[] headers) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .setRecordSeparator('\n')
                .setNullString(null)
                .build();
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath, fileName));
        return new CSVPrinter(writer, format);
    }

    // --- 데이터 생성 메서드 ---
    // tags.csv 파일은 여기서 한 번에 생성합니다. (데이터 양이 적기 때문)
    private List<Tag> generateTags(List<String> tagNames) throws IOException {
        AtomicLong idCounter = new AtomicLong(1);
        List<Tag> tags = tagNames.stream()
                .map(name -> new Tag(idCounter.getAndIncrement(), name, Timestamp.valueOf(LocalDateTime.now()), null))
                .collect(Collectors.toList());

        try (CSVPrinter tagPrinter = createCsvPrinter("tags.csv", Tag.HEADERS)) {
            for (Tag tag : tags) {
                tagPrinter.printRecord(tag.toCsvRow());
            }
        }
        return tags;
    }

    public void customGenerate(int userCount, int momentIterCountMax) throws IOException {
        List<Tag> tags = generateTags(Arrays.asList("일상/생각", "인간관계", "일/성장", "건강/운동", "취미/여가"));

        // users는 다른 데이터 생성에 필요하므로 메모리에 생성합니다.
        for (long i = 1; i <= userCount; i++) {
            users.add(new User(
                    i,
                    i + faker.internet().safeEmailAddress(),
                    faker.internet().password(),
                    faker.name().username() + i, // Unique nickname
                    Timestamp.valueOf(LocalDateTime.now()),
                    faker.number().numberBetween(0, 1000),
                    providerTypes[faker.number().numberBetween(0, 2)],
                    "ASTEROID_WHITE",
                    faker.number().numberBetween(0, 5000),
                    null
            ));
        }

        // --- 변경점: 모든 CSV 파일을 미리 열어둡니다 ---
        try (
                CSVPrinter userPrinter = createCsvPrinter("users.csv", User.HEADERS);
                CSVPrinter momentPrinter = createCsvPrinter("moments.csv", Moment.HEADERS);
                CSVPrinter momentImagePrinter = createCsvPrinter("momentImages.csv", MomentImage.HEADERS);
                CSVPrinter momentTagPrinter = createCsvPrinter("momentTags.csv", MomentTag.HEADERS);
                CSVPrinter commentPrinter = createCsvPrinter("comments.csv", Comment.HEADERS);
                CSVPrinter commentImagePrinter = createCsvPrinter("commentImages.csv", CommentImage.HEADERS);
                CSVPrinter echoPrinter = createCsvPrinter("echos.csv", Echo.HEADERS)
        ) {
            // users.csv 파일 쓰기
            for (User user : users) {
                userPrinter.printRecord(user.toCsvRow());
            }

            String[] writeType = {"BASIC", "EXTRA"};

            long momentId = 1L;
            long mimageId = 1L;
            long commentId = 1L;
            long cimageId = 1L;
            long echoId = 1L;
            long tagId = 1L;

            for (User user : users) {
                int momentIterCount = faker.number().numberBetween(0, momentIterCountMax);

                for (int i = 0; i < momentIterCount; i++) {
                    Instant endExclusive = Instant.now().minus(10, ChronoUnit.HOURS);
                    Instant startInclusive = Instant.now().minus(3 * 365, ChronoUnit.DAYS);

                    if (startInclusive.isAfter(endExclusive)) {
                        throw new IllegalArgumentException("시작 시점이 종료 시점보다 미래일 수 없습니다.");
                    }

                    java.util.Date randomDate = faker.date().between(
                            java.util.Date.from(startInclusive),
                            java.util.Date.from(endExclusive)
                    );

                    Timestamp createdAt = Timestamp.from(randomDate.toInstant());
                    Timestamp deletedAt = deleteByProbability(createdAt, 0.05);

                    Moment moment = new Moment(
                            momentId++,
                            user.id(), // FK
                            faker.lorem().sentence(10),
                            false,
                            createdAt,
                            writeType[faker.number().numberBetween(0, 2)],
                            deletedAt);
                    // --- 변경점: List에 추가하는 대신 즉시 파일에 씁니다 ---
                    momentPrinter.printRecord(moment.toCsvRow());

                    if (faker.random().nextDouble() >= 0.5) {
                        MomentImage momentImage = new MomentImage(
                                mimageId++,
                                moment.id(), // FK
                                faker.internet().url(),
                                faker.file().fileName(),
                                moment.createdAt(),
                                deletedAt);
                        momentImagePrinter.printRecord(momentImage.toCsvRow());
                    }

                    int tagIterCount = faker.number().numberBetween(1, 4);

                    for (int j = 0; j < tagIterCount; j++) {
                        MomentTag momentTag = new MomentTag(
                                tagId++,
                                moment.id(), // FK
                                tags.get(j).id(), // FK
                                moment.createdAt(),
                                deletedAt);
                        momentTagPrinter.printRecord(momentTag.toCsvRow());
                    }
                    int commentIterCount = faker.number().numberBetween(0, 20);

                    List<User> candidates = users.stream()
                            .filter(u -> !u.equals(user))
                            .collect(Collectors.toCollection(LinkedList::new));

                    Collections.shuffle(candidates);

                    for (int j = 0; j < commentIterCount; j++) {
                        java.util.Date referenceDate = moment.createdAt();
                        java.util.Date futureDate = faker.date().future(1, TimeUnit.HOURS, referenceDate);

                        Timestamp commentCreatedAt = Timestamp.from(futureDate.toInstant());
                        Timestamp commentDeletedAt = deleteByProbability(createdAt, 0.05);

                        User randomCommenter = candidates.removeFirst();

                        Comment comment = new Comment(
                                commentId++,
                                randomCommenter.id(), // FK
                                moment.id(), // FK
                                faker.lorem().sentence(5),
                                commentCreatedAt,
                                commentDeletedAt);
                        commentPrinter.printRecord(comment.toCsvRow());

                        if (faker.random().nextDouble() >= 0.5) {
                            CommentImage commentImage = new CommentImage(
                                    cimageId++,
                                    comment.id(), // FK
                                    faker.internet().url(),
                                    faker.file().fileName(),
                                    commentCreatedAt,
                                    commentDeletedAt);
                            commentImagePrinter.printRecord(commentImage.toCsvRow());
                        }

                        int echoIterCount = faker.number().numberBetween(0, 7);
                        java.util.Date echoFutureDate = faker.date().future(1, TimeUnit.HOURS, commentCreatedAt);
                        for (int k = 0; k < echoIterCount; k++) {
                            Echo echo = new Echo(
                                    echoId++,
                                    user.id(), // FK
                                    comment.id(), // FK
                                    Timestamp.from(echoFutureDate.toInstant()),
                                    echoTypes[k],
                                    commentDeletedAt
                            );
                            echoPrinter.printRecord(echo.toCsvRow());
                        }
                    }
                }
            }
        } // try-with-resources가 끝나면서 모든 파일이 자동으로 닫힙니다.
    }

    public Timestamp deleteByProbability(Timestamp createdAt, double probability) {
        if (faker.random().nextDouble() >= probability) {
            return null;
        }
        Instant startInclusive = createdAt.toInstant();
        Instant endExclusive = Instant.now();
        if (startInclusive.isAfter(endExclusive)) {
            return null;
        }
        Date randomDeletedDate = faker.date().between(
                Date.from(startInclusive),
                Date.from(endExclusive)
        );
        return Timestamp.from(randomDeletedDate.toInstant());
    }

    // --- 변경점: 이 메서드는 더 이상 사용되지 않습니다 ---
    // private <T> void writeToCsv(...) { ... }

    // CSV 행으로 변환하는 인터페이스
    interface CsvRecord {
        Iterable<?> toCsvRow();
    }

    // --- Record 정의는 변경할 필요가 없습니다 ---
    // DDL에 맞게 컬럼 순서를 정확히 맞춰야 합니다.
    record User(Long id, String email, String password, String nickname, Timestamp createdAt, int availableStar,
                String providerType, String level, int expStar, Timestamp deletedAt) implements CsvRecord {
        static final String[] HEADERS = {"id", "email", "password", "nickname", "created_at", "available_star",
                "provider_type", "level", "exp_star", "deleted_at"};

        public Iterable<?> toCsvRow() {
            return Arrays.asList(id, email, password, nickname, createdAt, availableStar, providerType, level, expStar,
                    deletedAt);
        }
    }

    record Tag(Long id, String name, Timestamp createdAt, Timestamp deletedAt) implements CsvRecord {
        static final String[] HEADERS = {"id", "name", "created_at", "deleted_at"};

        public Iterable<?> toCsvRow() {
            return Arrays.asList(id, name, createdAt, deletedAt);
        }
    }

    record Moment(Long id, Long momenterId, String content, boolean isMatched, Timestamp createdAt,
                  String writeType, Timestamp deletedAt) implements CsvRecord {
        static final String[] HEADERS = {"id", "momenter_id", "content", "is_matched", "created_at", "write_type",
                "deleted_at"};

        public Iterable<?> toCsvRow() {
            return Arrays.asList(id, momenterId, content, isMatched, createdAt, writeType, deletedAt);
        }
    }

    record MomentImage(Long id, Long momentId, String url, String originalName, Timestamp createdAt,
                       Timestamp deletedAt) implements
            CsvRecord {
        static final String[] HEADERS = {"id", "moment_id", "url", "original_name", "created_at", "deleted_at"};

        public Iterable<?> toCsvRow() {
            return Arrays.asList(id, momentId, url, originalName, createdAt, deletedAt);
        }
    }

    record MomentTag(Long id, Long momentId, Long tagId, Timestamp createdAt, Timestamp deletedAt) implements
            CsvRecord {
        static final String[] HEADERS = {"id", "moment_id", "tag_id", "created_at", "deleted_at"};

        public Iterable<?> toCsvRow() {
            return Arrays.asList(id, momentId, tagId, createdAt, deletedAt);
        }
    }

    record Comment(Long id, Long commenterId, Long momentId, String content, Timestamp createdAt,
                   Timestamp deletedAt) implements CsvRecord {
        static final String[] HEADERS = {"id", "commenter_id", "moment_id", "content", "created_at", "deleted_at"};

        public Iterable<?> toCsvRow() {
            return Arrays.asList(id, commenterId, momentId, content, createdAt, deletedAt);
        }
    }

    record CommentImage(Long id, Long commentId, String url, String originalName, Timestamp createdAt,
                        Timestamp deletedAt) implements
            CsvRecord {
        static final String[] HEADERS = {"id", "comment_id", "url", "original_name", "created_at", "deleted_at"};

        public Iterable<?> toCsvRow() {
            return Arrays.asList(id, commentId, url, originalName, createdAt, deletedAt);
        }
    }

    record Echo(Long id, Long userId, Long commentId, Timestamp createdAt, String type, Timestamp deletedAt) implements
            CsvRecord {
        static final String[] HEADERS = {"id", "user_id", "comment_id", "created_at", "type", "deleted_at"};

        public Iterable<?> toCsvRow() {
            return Arrays.asList(id, userId, commentId, createdAt, type, deletedAt);
        }
    }
}
