package com.Eventora.repository;

import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.EventWatchlist;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface EventWatchlistRepository extends JpaRepository<EventWatchlist,Long> {

//    @Transactional
//    @Query(value = "delete from event_watchlist where user_id = :userID and event_id = :eventID",nativeQuery = true)
//    long deleteByUserAndEventId(Long userID, Long eventID);

    @Transactional
    void deleteByUser_IdAndEvent_Id(Long userId, Long eventId);



    @Query(value = "select event_id from event_watchlist where user_id = :appUserId",nativeQuery = true)
    List<Long> getLikedEventsIdsByUser(@Param("appUserId") Long appUserId);
}
