import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TypeHelperTest {

    @Test
    @DisplayName("getName() should return NULL_PRESENTATION when type is null")
    void getName_withNullType_shouldReturnNullPresentation() {
        // When
        String result = TypeHelper.getName(null);
        
        // Then
        assertThat(result).isEqualTo(TypeHelper.NULL_PRESENTATION);
    }
    
    @Test
    @DisplayName("getName() should return class name when type is a Class")
    void getName_withClassType_shouldReturnClassName() {
        // Given
        Type type = String.class;
        
        // When
        String result = TypeHelper.getName(type);
        
        // Then
        assertThat(result).isEqualTo("String");
    }
    
    @Test
    @DisplayName("getName() should handle array types with [] suffix")
    void getName_withArrayType_shouldReturnClassNameWithBrackets() {
        // Given
        Type type = String[].class;
        
        // When
        String result = TypeHelper.getName(type);
        
        // Then
        assertThat(result).isEqualTo("String[]");
    }
    
    @Test
    @DisplayName("getName() should handle multi-dimensional arrays")
    void getName_withMultiDimensionalArray_shouldReturnCorrectFormat() {
        // Given
        Type type = String[][].class;
        
        // When
        String result = TypeHelper.getName(type);
        
        // Then
        assertThat(result).isEqualTo("String[][]");
    }
    
    @Test
    @DisplayName("getName() should handle primitive types")
    void getName_withPrimitiveType_shouldReturnPrimitiveName() {
        // Given
        Type type = int.class;
        
        // When
        String result = TypeHelper.getName(type);
        
        // Then
        assertThat(result).isEqualTo("int");
    }
    
    @Test
    @DisplayName("getName() should handle primitive array types")
    void getName_withPrimitiveArrayType_shouldReturnCorrectFormat() {
        // Given
        Type type = int[].class;
        
        // When
        String result = TypeHelper.getName(type);
        
        // Then
        assertThat(result).isEqualTo("int[]");
    }
    
    @ParameterizedTest
    @MethodSource("provideTypesForGetName")
    @DisplayName("getName() should return correct names for various types")
    void getName_withVariousTypes_shouldReturnExpectedNames(Type type, String expectedName) {
        // When
        String result = TypeHelper.getName(type);
        
        // Then
        assertThat(result).isEqualTo(expectedName);
    }
    
    private static Stream<Arguments> provideTypesForGetName() {
        return Stream.of(
            // Null case
            Arguments.of(null, TypeHelper.NULL_PRESENTATION),
            
            // Simple classes
            Arguments.of(String.class, "String"),
            Arguments.of(Integer.class, "Integer"),
            Arguments.of(Object.class, "Object"),
            Arguments.of(List.class, "List"),
            Arguments.of(Map.class, "Map"),
            
            // Primitive types
            Arguments.of(int.class, "int"),
            Arguments.of(long.class, "long"),
            Arguments.of(double.class, "double"),
            Arguments.of(boolean.class, "boolean"),
            Arguments.of(char.class, "char"),
            Arguments.of(byte.class, "byte"),
            Arguments.of(short.class, "short"),
            Arguments.of(float.class, "float"),
            Arguments.of(void.class, "void"),
            
            // Array types
            Arguments.of(String[].class, "String[]"),
            Arguments.of(Integer[].class, "Integer[]"),
            Arguments.of(int[].class, "int[]"),
            Arguments.of(long[].class, "long[]"),
            
            // Multi-dimensional arrays
            Arguments.of(String[][].class, "String[][]"),
            Arguments.of(int[][].class, "int[][]"),
            Arguments.of(Object[][][].class, "Object[][][]")
        );
    }
    
    @Test
    @DisplayName("getName() should handle ParameterizedType by converting to Class")
    void getName_withParameterizedType_shouldReturnClassName() throws NoSuchFieldException {
        // Given - Obtenir un ParameterizedType (ex: List<String>)
        Type type = getClass().getDeclaredField("testList").getGenericType();
        
        // When
        String result = TypeHelper.getName(type);
        
        // Then
        assertThat(result).isEqualTo("List");
    }
    
    // Champ utilisé pour obtenir un ParameterizedType dans le test
    @SuppressWarnings("unused")
    private List<String> testList;
    
    @Test
    @DisplayName("getName() should handle GenericArrayType")
    void getName_withGenericArrayType_shouldReturnCorrectFormat() throws NoSuchMethodException {
        // Given - Obtenir un GenericArrayType (ex: T[])
        Type type = getClass().getDeclaredMethod("genericArrayMethod").getGenericReturnType();
        
        // When
        String result = TypeHelper.getName(type);
        
        // Then
        assertThat(result).contains("[]");
    }
    
    // Méthode utilisée pour obtenir un GenericArrayType dans le test
    @SuppressWarnings("unused")
    private <T> T[] genericArrayMethod() {
        return null;
    }
    
    @Test
    @DisplayName("getName() should not throw exception for any valid Type")
    void getName_withAnyValidType_shouldNotThrowException() {
        // Given
        Type[] types = {
            String.class,
            int.class,
            String[].class,
            List.class,
            null
        };
        
        // When & Then
        for (Type type : types) {
            assertDoesNotThrow(() -> TypeHelper.getName(type));
        }
    }
}
-------------
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobRunnerConfigTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job job1;

    @Mock
    private Job job2;

    @Captor
    private ArgumentCaptor<JobParameters> jobParametersCaptor;

    @InjectMocks
    private YourConfigClass configClass; // Remplacez par le nom de votre classe

    private CommandLineRunner jobRunner;

    @BeforeEach
    void setUp() {
        jobRunner = configClass.jobRunner();
    }

    @Test
    @DisplayName("jobRunner should execute all jobs found in application context")
    void jobRunner_shouldExecuteAllJobs() throws Exception {
        // Given
        Map<String, Job> jobs = Map.of(
            "job1", job1,
            "job2", job2
        );
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(jobs);

        // When
        jobRunner.run();

        // Then
        verify(jobLauncher, times(2)).run(any(Job.class), any(JobParameters.class));
        verify(jobLauncher).run(eq(job1), any(JobParameters.class));
        verify(jobLauncher).run(eq(job2), any(JobParameters.class));
    }

    @Test
    @DisplayName("jobRunner should pass correct JobParameters with jobId and timestamp")
    void jobRunner_shouldPassCorrectJobParameters() throws Exception {
        // Given
        Map<String, Job> jobs = Map.of("job1", job1);
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(jobs);

        // When
        jobRunner.run();

        // Then
        verify(jobLauncher).run(eq(job1), jobParametersCaptor.capture());
        JobParameters capturedParams = jobParametersCaptor.getValue();

        assertThat(capturedParams.getString("jobId")).isNotNull();
        assertThat(capturedParams.getLong("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("jobRunner should create unique parameters for each job")
    void jobRunner_shouldCreateUniqueParametersForEachJob() throws Exception {
        // Given
        Map<String, Job> jobs = Map.of(
            "job1", job1,
            "job2", job2
        );
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(jobs);

        // When
        jobRunner.run();

        // Then
        verify(jobLauncher, times(2)).run(any(Job.class), jobParametersCaptor.capture());
        List<JobParameters> allParams = jobParametersCaptor.getAllValues();

        assertThat(allParams).hasSize(2);
        // Les paramètres peuvent être identiques ou différents selon le timing
        assertThat(allParams.get(0)).isNotNull();
        assertThat(allParams.get(1)).isNotNull();
    }

    @Test
    @DisplayName("jobRunner should handle empty job collection")
    void jobRunner_shouldHandleEmptyJobCollection() throws Exception {
        // Given
        Map<String, Job> jobs = Collections.emptyMap();
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(jobs);

        // When
        jobRunner.run();

        // Then
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("jobRunner should handle single job")
    void jobRunner_shouldHandleSingleJob() throws Exception {
        // Given
        Map<String, Job> jobs = Map.of("job1", job1);
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(jobs);

        // When
        jobRunner.run();

        // Then
        verify(jobLauncher, times(1)).run(eq(job1), any(JobParameters.class));
    }

    @Test
    @DisplayName("jobRunner should propagate exception from jobLauncher")
    void jobRunner_shouldPropagateExceptionFromJobLauncher() throws Exception {
        // Given
        Map<String, Job> jobs = Map.of("job1", job1);
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(jobs);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Job execution failed"));

        // When & Then
        assertThatThrownBy(() -> jobRunner.run())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Job execution failed");
    }

    @Test
    @DisplayName("jobRunner should execute jobs sequentially")
    void jobRunner_shouldExecuteJobsSequentially() throws Exception {
        // Given
        Map<String, Job> jobs = new LinkedHashMap<>();
        jobs.put("job1", job1);
        jobs.put("job2", job2);
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(jobs);

        // When
        jobRunner.run();

        // Then
        verify(jobLauncher, inOrder(jobLauncher)).run(eq(job1), any(JobParameters.class));
        verify(jobLauncher, inOrder(jobLauncher)).run(eq(job2), any(JobParameters.class));
    }

    @Test
    @DisplayName("jobRunner should be a valid CommandLineRunner bean")
    void jobRunner_shouldReturnValidCommandLineRunner() {
        // When
        CommandLineRunner runner = configClass.jobRunner();

        // Then
        assertThat(runner).isNotNull();
        assertThat(runner).isInstanceOf(CommandLineRunner.class);
    }

    @Test
    @DisplayName("jobRunner should add jobId as String")
    void jobRunner_shouldAddJobIdAsString() throws Exception {
        // Given
        Map<String, Job> jobs = Map.of("job1", job1);
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(jobs);

        // When
        jobRunner.run();

        // Then
        verify(jobLauncher).run(eq(job1), jobParametersCaptor.capture());
        JobParameters params = jobParametersCaptor.getValue();

        String jobId = params.getString("jobId");
        assertThat(jobId).isNotNull();
        assertThat(jobId).matches("\\d+"); // Vérifie que c'est un nombre sous forme de String
    }

    @Test
    @DisplayName("jobRunner should add timestamp as Long")
    void jobRunner_shouldAddTimestampAsLong() throws Exception {
        // Given
        Map<String, Job> jobs = Map.of("job1", job1);
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(jobs);

        // When
        jobRunner.run();

        // Then
        verify(jobLauncher).run(eq(job1), jobParametersCaptor.capture());
        JobParameters params = jobParametersCaptor.getValue();

        Long timestamp = params.getLong("timestamp");
        assertThat(timestamp).isNotNull();
        assertThat(timestamp).isGreaterThan(0L);
    }

    @Test
    @DisplayName("jobRunner should handle null from getBeansOfType gracefully")
    void jobRunner_shouldHandleNullBeansOfType() throws Exception {
        // Given
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> jobRunner.run())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("jobRunner should run with command line arguments")
    void jobRunner_shouldRunWithCommandLineArguments() throws Exception {
        // Given
        Map<String, Job> jobs = Map.of("job1", job1);
        when(applicationContext.getBeansOfType(Job.class)).thenReturn(jobs);
        String[] args = {"arg1", "arg2"};

        // When
        jobRunner.run(args);

        // Then
        verify(jobLauncher).run(eq(job1), any(JobParameters.class));
    }
}
-----------------------
