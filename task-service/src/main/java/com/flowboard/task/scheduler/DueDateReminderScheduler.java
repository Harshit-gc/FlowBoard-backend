package com.flowboard.task.scheduler;

import com.flowboard.task.entity.Card;
import com.flowboard.task.messaging.NotificationEvent;
import com.flowboard.task.messaging.NotificationPublisher;
import com.flowboard.task.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DueDateReminderScheduler {

    private final CardRepository cardRepository;
    private final NotificationPublisher notificationPublisher;

    // Runs every day at 9:00 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDueDateReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        log.info("Running due date reminder job for date: {}", tomorrow);

        List<Card> cardsDueTomorrow = cardRepository
                .findByDueDateAndIsArchivedFalse(tomorrow);

        for (Card card : cardsDueTomorrow) {
            if (card.getStatus() == Card.Status.DONE) continue;

            if (card.getAssigneeId() == null) continue;

            notificationPublisher.publish(NotificationEvent.builder()
                    .recipientId(card.getAssigneeId())
                    .actorId(card.getCreatedById())
                    .type("DUE_DATE")
                    .title("Card due tomorrow")
                    .message("\"" + card.getTitle() +
                            "\" is due tomorrow on " + tomorrow)
                    .relatedId(card.getCardId())
                    .relatedType("CARD")
                    .deepLinkUrl("/board/" + card.getBoardId())
                    .build());

            log.info("Sent due date reminder for cardId={} to userId={}",
                    card.getCardId(), card.getAssigneeId());
        }
    }
}