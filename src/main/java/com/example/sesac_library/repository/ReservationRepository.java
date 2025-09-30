package com.example.sesac_library.repository;

import com.example.sesac_library.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserIdAndStatus(Long userId, String status);

    List<Reservation> findByBookIdAndStatus(Long bookId, String status);

    Optional<Reservation> findByUserIdAndBookIdAndStatus(Long userId, Long bookId, String status);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE'")
    List<Reservation> findActiveReservations();

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.bookId = :bookId AND r.status = 'ACTIVE'")
    Long countActiveReservationsByBookId(@Param("bookId") Long bookId);

    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId AND r.status = 'ACTIVE'")
    List<Reservation> findActiveReservationsByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Reservation r WHERE r.bookId = :bookId AND r.status = 'ACTIVE' ORDER BY r.reservationDate ASC")
    List<Reservation> findActiveReservationsByBookIdOrderByDate(@Param("bookId") Long bookId);
}