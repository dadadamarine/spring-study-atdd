package codesquad.web;

import codesquad.domain.Answer;
import codesquad.domain.Question;
import codesquad.domain.QuestionDTO;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import support.test.AcceptanceTest;

import static codesquad.domain.UserTest.SANJIGI;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiQuestionAcceptanceTest extends AcceptanceTest {
    private static final Logger log = LoggerFactory.getLogger(ApiQuestionAcceptanceTest.class);
    private static final String URL_API_QUESTION = "/api/questions";

    @Test
    public void list() {
        ResponseEntity<Iterable> response = template().getForEntity(URL_API_QUESTION, Iterable.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        log.debug("body : {}", response.getBody());
    }

    @Test
    public void create() {
        QuestionDTO newQuestionDTO = new QuestionDTO("new title", "new context");
        String location = createResourceWithAuth(URL_API_QUESTION, newQuestionDTO);

        assertThat(getResource(location, Question.class, defaultUser())).isNotNull();
        assertThat(getResource(location, Question.class, defaultUser()).getTitle()).isEqualTo("new title");

    }

    @Test
    public void create_not_login() {
        QuestionDTO newQuestionDTO = new QuestionDTO("new title1", "new context1");
        ResponseEntity<Void> response = template().postForEntity(URL_API_QUESTION, newQuestionDTO, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void detail() {
        QuestionDTO newQuestion = new QuestionDTO("new title2", "new context2");
        String location = createResourceWithAuth(URL_API_QUESTION, newQuestion);
        Question question = template().getForObject(location, Question.class);

        assertThat(question).isNotNull();
    }

    @Test
    public void update() {
        QuestionDTO newQuestionDTO = new QuestionDTO("new title3", "new context3");
        String location = createResourceWithAuth(URL_API_QUESTION, newQuestionDTO);
        Question original = basicAuthTemplate().getForObject(location, Question.class);
        QuestionDTO updateQuestionDto = new QuestionDTO("update3", "update3");

        ResponseEntity<Question> response = basicAuthTemplate().exchange(location, HttpMethod.PUT, createHttpEntity(updateQuestionDto), Question.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo(updateQuestionDto.getTitle());
        assertThat(response.getBody().getContents()).isEqualTo(updateQuestionDto.getContent());

    }

    @Test
    public void update_not_login() {
        QuestionDTO newQuestion = new QuestionDTO("new title4", "new context4");
        String location = createResourceWithAuth(URL_API_QUESTION, newQuestion);
        Question original = basicAuthTemplate().getForObject(location, Question.class);
        QuestionDTO updateQuestionDto = new QuestionDTO("update4", "update4");

        ResponseEntity<Question> response = template().exchange(location, HttpMethod.PUT, createHttpEntity(new QuestionDTO("update4", "update4")), Question.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(original.getTitle()).isEqualTo(template().getForObject(location, Question.class).getTitle());
    }

    @Test
    public void update_not_match_user() {
        QuestionDTO newQuestion = new QuestionDTO("new title5", "new context5");
        String location = createResourceWithAuth(URL_API_QUESTION, newQuestion);
        Question original = basicAuthTemplate().getForObject(location, Question.class);
        QuestionDTO updateQuestionDto = new QuestionDTO("update5", "update5");

        ResponseEntity<Question> response = basicAuthTemplate(SANJIGI).exchange(location, HttpMethod.PUT, createHttpEntity(updateQuestionDto), Question.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(original.getTitle()).isEqualTo(template().getForObject(location, Question.class).getTitle());
    }

    @Test
    public void delete() throws Exception {
        QuestionDTO newQuestion = new QuestionDTO("new title6", "new context6");
        String location = createResourceWithAuth(URL_API_QUESTION, newQuestion);

        ResponseEntity<Void> response = basicAuthTemplate().exchange(location, HttpMethod.DELETE, createHttpEntity(null), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(basicAuthTemplate().getForEntity(location, Question.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void delete_not_login() {
        QuestionDTO newQuestion = new QuestionDTO("new title7", "new context7");
        String location = createResourceWithAuth(URL_API_QUESTION, newQuestion);

        ResponseEntity<Void> response = template().exchange(location, HttpMethod.DELETE, createHttpEntity(null), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(template().getForObject(location, Question.class).isDeleted()).isFalse();
    }

    @Test
    public void delete_not_match_user() {
        QuestionDTO newQuestion = new QuestionDTO("new title8", "new context8");
        String location = createResourceWithAuth(URL_API_QUESTION, newQuestion);

        ResponseEntity<Void> response = basicAuthTemplate(SANJIGI).exchange(location, HttpMethod.DELETE, createHttpEntity(null), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(template().getForObject(location, Question.class)).isNotNull();
    }


    @Test
    public void delete_question_with_answer_by_other() {
        ResponseEntity<Void> response = basicAuthTemplate().exchange(URL_API_QUESTION + "/1", HttpMethod.DELETE, createHttpEntity(null), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Answer> answerResponse = basicAuthTemplate().getForEntity(URL_API_QUESTION + "/1", Answer.class);
        assertThat(answerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(answerResponse.getBody().getContents()).isNotEmpty();
    }

    @Test
    public void delete_question_with_only_answer_by_writer() {
        QuestionDTO newQuestion = new QuestionDTO("new title8", "new context8");
        String location = createResourceWithAuth(URL_API_QUESTION, newQuestion);
        String answerLocation = createResourceWithAuth(location + "/answers", "answer contents");

        ResponseEntity<Void> response = basicAuthTemplate().exchange(location, HttpMethod.DELETE, createHttpEntity(null), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Answer> answerResponse = basicAuthTemplate().getForEntity(answerLocation, Answer.class);
        assertThat(answerResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void delete_not_found_question() {
        //given
        Long notFoundId = 10000L;
        //when
        ResponseEntity<Void> response = basicAuthTemplate().exchange(URL_API_QUESTION + "/" + notFoundId, HttpMethod.DELETE, createHttpEntity(null), Void.class);
        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
