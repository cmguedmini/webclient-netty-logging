import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogJobExecutionListenerTest {

    private LogJobExecutionListener listener;
    private Logger logbackLogger;
    private ListAppender<ILoggingEvent> listAppender;

    private static final String JOB_NAME = "TestJob";

    @BeforeEach
    void setUp() {
        // 1. Initialise l'instance de la classe à tester
        listener = new LogJobExecutionListener();
        
        // 2. Récupère l'instance Logback Logger pour notre classe
        logbackLogger = (Logger) LoggerFactory.getLogger(LogJobExecutionListener.class);
        
        // 3. Crée un ListAppender pour capturer les événements de log
        listAppender = new ListAppender<>();
        listAppender.start();
        
        // 4. Attache le ListAppender au Logger
        logbackLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        // Détache l'appender après chaque test pour éviter les fuites de logs
        logbackLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    // Méthode utilitaire simple pour créer un mock JobExecution
    private JobExecution createMockJobExecution(BatchStatus status) {
        JobExecution jobExecution = mock(JobExecution.class);
        JobInstance jobInstance = mock(JobInstance.class);
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn(JOB_NAME);
        when(jobExecution.getStatus()).thenReturn(status);
        return jobExecution;
    }

    // -------------------------------------------------------------------------------------------------
    // TESTS UNITAIRES
    // -------------------------------------------------------------------------------------------------

    ## 1. Test Unitaire pour `beforeJob`

    @Test
    void beforeJob_ShouldLogJobStartInfo() {
        // GIVEN
        JobExecution jobExecution = createMockJobExecution(BatchStatus.STARTED);

        // WHEN
        listener.beforeJob(jobExecution);

        // THEN
        // Vérifie qu'exactement un message a été logué
        assertThat(listAppender.list).hasSize(1);
        
        // Vérifie le contenu du log
        ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        // Le message logué doit contenir les informations attendues
        assertThat(loggingEvent.getFormattedMessage()).contains("JOB (TestJob) STARTED: STARTED");
    }

    ---

    ## 2. Tests Unitaires pour `afterJob`

    ### Test 2.1 : Job COMPLETED (Log INFO)

    @Test
    void afterJob_ShouldLogInfo_WhenCompleted() {
        // GIVEN
        JobExecution jobExecution = createMockJobExecution(BatchStatus.COMPLETED);

        // WHEN
        listener.afterJob(jobExecution);

        // THEN
        // Vérifie le contenu du log
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent.getFormattedMessage()).contains("JOB (TestJob) COMPLETED");
    }

    ### Test 2.2 : Job FAILED (Log WARN)

    @Test
    void afterJob_ShouldLogWarn_WhenNotCompleted() {
        // GIVEN
        JobExecution jobExecution = createMockJobExecution(BatchStatus.FAILED); // Status différent de COMPLETED

        // WHEN
        listener.afterJob(jobExecution);

        // THEN
        // Vérifie le contenu du log
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(loggingEvent.getFormattedMessage()).contains("JOB (TestJob) is not COMPLETED, check this status : FAILED");
    }
}
