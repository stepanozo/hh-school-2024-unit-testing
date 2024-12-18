package ru.hh.school.unittesting.libraryManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.school.unittesting.homework.LibraryManager;
import ru.hh.school.unittesting.homework.NotificationService;
import ru.hh.school.unittesting.homework.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserService userService;
    @InjectMocks
    private LibraryManager libraryManager;


    @Test
    void addBooksGetAvailableCopiesTest(){
        libraryManager.addBook("Преступление и наказание", 3);
        libraryManager.addBook("Преступление и наказание", 1);
        libraryManager.addBook("Обломов", 2);

        assertEquals(4, libraryManager.getAvailableCopies("Преступление и наказание"));
        assertEquals(2, libraryManager.getAvailableCopies("Обломов"));
    }

    @ParameterizedTest
    @CsvSource({
            "Идиот, stepanozo",
            "Лжец на кушетке, Kirill030",
            "Обломов, stepanozo"
    })
    void borrowBookSuccessTest(String bookId, String userId){
        libraryManager.addBook(bookId, 1);
        when(userService.isUserActive(userId)).thenReturn(true);
        assertTrue(libraryManager.borrowBook(bookId, userId));
        verify(userService, times(1)).isUserActive(userId);
        verify(notificationService, times(1)).notifyUser(userId, "You have borrowed the book: " + bookId);
    }

    @Test
    void borrowBooksBookEndedTest() {
        libraryManager.addBook("Лжец на кушетке", 2);
        when(userService.isUserActive("stepanozo")).thenReturn(true);

        assertTrue(libraryManager.borrowBook("Лжец на кушетке", "stepanozo"));
        assertTrue(libraryManager.borrowBook("Лжец на кушетке", "stepanozo"));
        assertFalse(libraryManager.borrowBook("Лжец на кушетке", "stepanozo"));

        verify(userService, times(3)).isUserActive("stepanozo");
        verify(notificationService, times(2)).notifyUser("stepanozo", "You have borrowed the book: Лжец на кушетке");
    }

    @Test
    void borrowBookInactiveUserTest(){
        when(userService.isUserActive("stepanozo")).thenReturn(false);

        assertFalse(libraryManager.borrowBook("Преступление и наказание", "stepanozo"));
        verify(userService, times(1)).isUserActive("stepanozo");
    }

    @Test
    void borrowBookNoAvailableCopiesTest(){
        assertFalse(libraryManager.borrowBook("Преступление и наказание", "stepanozo"));
    }

    @ParameterizedTest
    @CsvSource({
            "Идиот, stepanozo",
            "Преступление и наказание, Kirill030",
            "Обломов, Xavier"
    })
    void returnBookFailTest(String bookId, String userId){
        when(userService.isUserActive("stepanozo")).thenReturn(true);
        libraryManager.addBook("Преступление и наказание", 1);
        libraryManager.borrowBook("Преступление и наказание", "stepanozo");
        assertFalse(libraryManager.returnBook(bookId, userId));
    }

    @Test
    void returnBookSuccessTest(){
        when(userService.isUserActive("stepanozo")).thenReturn(true);
        libraryManager.addBook("Преступление и наказание", 1);
        assertTrue(libraryManager.borrowBook("Преступление и наказание", "stepanozo"));
        assertTrue(libraryManager.returnBook("Преступление и наказание", "stepanozo"));

        verify(userService, times(1)).isUserActive("stepanozo");
        verify(notificationService, times(1)).notifyUser("stepanozo", "You have borrowed the book: Преступление и наказание");
        verify(notificationService, times(1)).notifyUser("stepanozo", "You have returned the book: Преступление и наказание");
    }

    @Test
    void borrowAfterReturnTest(){
        when(userService.isUserActive("stepanozo")).thenReturn(true);
        when(userService.isUserActive("Kirill030")).thenReturn(true);
        libraryManager.addBook("Преступление и наказание", 1);
        assertTrue(libraryManager.borrowBook("Преступление и наказание", "stepanozo"));
        assertFalse(libraryManager.borrowBook("Преступление и наказание", "Kirill030"));
        assertTrue(libraryManager.returnBook("Преступление и наказание", "stepanozo"));
        assertTrue(libraryManager.borrowBook("Преступление и наказание", "Kirill030"));

        verify(userService, times(1)).isUserActive("stepanozo");
        verify(userService, times(2)).isUserActive("Kirill030");
        verify(notificationService, times(1)).notifyUser("stepanozo", "You have borrowed the book: Преступление и наказание");
        verify(notificationService, times(1)).notifyUser("stepanozo", "You have returned the book: Преступление и наказание");
        verify(notificationService, times(1)).notifyUser("Kirill030", "You have borrowed the book: Преступление и наказание");
    }

    @Test
    void calculateDynamicLateFeeExceptionTest(){
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> libraryManager.calculateDynamicLateFee(-1, true, false)
        );
        assertEquals("Overdue days cannot be negative.", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "2, true, true, 1.2",
            "3, false, true, 1.2",
            "5, true, false, 3.75",
            "7, false, false, 3.5",

    })
    void calculateDynamicLateFeeTest(int overdueDays, boolean isBestseller, boolean isPremiumMember, double expectedResult) {
        assertEquals(expectedResult, libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember));
    }
}
