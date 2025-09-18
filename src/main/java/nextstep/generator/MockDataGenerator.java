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
    List<User> users = new ArrayList<>();
    String[] providerTypes = {"GOOGLE", "EMAIL"};
    String[] echoTypes = {"SAD", "THANKS", "TOUCHED", "FUNNY", "COMFORTED", "AMAZING"};

    public MockDataGenerator(String outputPath) {
        this.outputPath = outputPath;
    }
    // --- 데이터 생성 메서드 ---

    private List<Tag> generateTags(List<String> tagNames) throws IOException {
        AtomicLong idCounter = new AtomicLong(1);
        List<Tag> tags = tagNames.stream()
                .map(name -> new Tag(idCounter.getAndIncrement(), name, Timestamp.valueOf(LocalDateTime.now()), null))
                .collect(
                        Collectors.toList());
        writeToCsv("tags.csv", Tag.HEADERS, tags);
        return tags;
    }

    public void customGenerate(int userCount, int momentIterCountMax) throws IOException {
        List<Tag> tags = generateTags(Arrays.asList("일상/생각", "인간관계", "일/성장", "건강/운동", "취미/여가"));

        for (long i = 1; i <= userCount; i++) { // 유저
            users.add(new User(
                    i,
                    faker.internet().safeEmailAddress(),
                    faker.internet().password(),
                    faker.name().username() + i, // Unique nickname
                    Timestamp.valueOf(LocalDateTime.now()),
                    faker.number().numberBetween(0, 1000),
                    providerTypes[faker.number().numberBetween(0, 2)],
                    "LV" + faker.number().numberBetween(1, 10),
                    faker.number().numberBetween(0, 5000),
                    null
            ));
        }

        List<Moment> moments = new ArrayList<>();
        List<MomentImage> momentImages = new ArrayList<>();
        List<MomentTag> momentTags = new ArrayList<>();
        List<Comment> comments = new ArrayList<>();
        List<CommentImage> commentImages = new ArrayList<>();
        List<Echo> echoes = new ArrayList<>();

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
                moments.add(moment);

                if (faker.random().nextDouble() >= 0.5) {
                    MomentImage momentImage = new MomentImage(
                            mimageId++,
                            moment.id(), // FK
                            faker.internet().url(),
                            faker.file().fileName(),
                            moment.createdAt(),
                            deletedAt);
                    momentImages.add(momentImage);
                }

                int tagIterCount = faker.number().numberBetween(1, 4);

                for (int j = 0; j < tagIterCount; j++) {
                    MomentTag momentTag = new MomentTag(
                            tagId++,
                            moment.id(), // FK
                            tags.get(j).id(), // FK
                            moment.createdAt(),
                            deletedAt);
                    momentTags.add(momentTag);
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
                    comments.add(comment);

                    if (faker.random().nextDouble() >= 0.5) {
                        CommentImage commentImage = new CommentImage(
                                cimageId++,
                                comment.id(), // FK
                                faker.internet().url(),
                                faker.file().fileName(),
                                commentCreatedAt,
                                commentDeletedAt);
                        commentImages.add(commentImage);
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
                        echoes.add(echo);
                    }
                }
            }
        }
        writeToCsv("users.csv", User.HEADERS, users);
        writeToCsv("tags.csv", Tag.HEADERS, tags);
        writeToCsv("moments.csv", Moment.HEADERS, moments);
        writeToCsv("momentImages.csv", MomentImage.HEADERS, momentImages);
        writeToCsv("momentTags.csv", MomentTag.HEADERS, momentTags);
        writeToCsv("comments.csv", Comment.HEADERS, comments);
        writeToCsv("commentImages.csv", CommentImage.HEADERS, commentImages);
        writeToCsv("echos.csv", Echo.HEADERS, echoes);
    }

    public Timestamp deleteByProbability(Timestamp createdAt, double probability) {
        // 1. 확률 체크 후 조건이 안 맞으면 즉시 null 반환 (Early Return)
        if (faker.random().nextDouble() >= probability) {
            return null;
        }
        // 2. 시간 범위를 명확하게 정의 (생성 시각 ~ 현재 시각)
        Instant startInclusive = createdAt.toInstant();
        Instant endExclusive = Instant.now();

        // 생성 시각이 현재보다 미래인 경우 오류 방지
        if (startInclusive.isAfter(endExclusive)) {
            return null;
        }

        // 3. Faker의 between() 메서드를 사용해 랜덤 Date 생성
        Date randomDeletedDate = faker.date().between(
                Date.from(startInclusive),
                Date.from(endExclusive)
        );

        // 4. 생성된 Date를 Timestamp로 변환하여 반환
        return Timestamp.from(randomDeletedDate.toInstant());
    }

    // --- 데이터 저장을 위한 Record 정의 ---

    private <T> void writeToCsv(String fileName, String[] headers, List<T> records) throws IOException {
        // CSV 포맷 설정 (MySQL 기본값과 유사하게)
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .setRecordSeparator('\n')
                .setNullString("\\N") // DB에서 NULL을 표현하는 방식
                .build();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath, fileName));
             CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {

            for (T record : records) {
                // 각 record 객체를 Iterable<String>으로 변환하여 출력
                csvPrinter.printRecord(((CsvRecord) record).toCsvRow());
            }
        }
    }

    // CSV 행으로 변환하는 인터페이스
    interface CsvRecord {
        Iterable<?> toCsvRow();
    }

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
