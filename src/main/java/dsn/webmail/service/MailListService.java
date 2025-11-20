package dsn.webmail.service;

import dsn.webmail.dto.MailDtos.MailDetailResponse;
import dsn.webmail.dto.MailDtos.MailListItemResponse;
import dsn.webmail.dto.MailDtos.MailListResponse;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.MailCategory;
import dsn.webmail.entity.ProcessedMail;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.repository.ProcessedMailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailListService {

    private final AppUserRepository appUserRepository;
    private final ProcessedMailRepository processedMailRepository;
    private final MailAnalyzerService mailAnalyzerService;

    @Transactional(readOnly = true)
    public MailListResponse getMailList(String email, int page, int size, MailCategory category) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        PageRequest pageRequest = PageRequest.of(page, size);

        Page<ProcessedMail> mailPage;
        if (category != null) {
            mailPage = processedMailRepository.findByUserIdAndCategoryOrderByProcessedAtDesc(
                    user.getId(), category, pageRequest);
        } else {
            mailPage = processedMailRepository.findByUserIdOrderByProcessedAtDesc(
                    user.getId(), pageRequest);
        }

        List<MailListItemResponse> mails = mailPage.getContent().stream()
                .map(this::toListItemResponse)
                .toList();

        return new MailListResponse(mails, mailPage.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public MailDetailResponse getMailDetail(String email, Long mailId) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        ProcessedMail mail = processedMailRepository.findByIdAndUserId(mailId, user.getId())
                .orElseThrow(() -> new RuntimeException("메일을 찾을 수 없습니다: " + mailId));

        return toDetailResponse(mail);
    }

    @Transactional
    public MailDetailResponse reanalyzeMail(String email, Long mailId) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        ProcessedMail mail = processedMailRepository.findByIdAndUserId(mailId, user.getId())
                .orElseThrow(() -> new RuntimeException("메일을 찾을 수 없습니다: " + mailId));

        mailAnalyzerService.analyzeMail(mail);

        return toDetailResponse(mail);
    }

    private MailListItemResponse toListItemResponse(ProcessedMail mail) {
        return new MailListItemResponse(
                mail.getId(),
                mail.getMessageId(),
                mail.getSubject(),
                mail.getFromAddress(),
                mail.getProcessedAt(),
                mail.getCategory(),
                mail.getCategory() != null ? mail.getCategory().getDisplayName() : null,
                mail.getCategoryConfidence(),
                mail.getSummary()
        );
    }

    private MailDetailResponse toDetailResponse(ProcessedMail mail) {
        return new MailDetailResponse(
                mail.getId(),
                mail.getMessageId(),
                mail.getSubject(),
                mail.getFromAddress(),
                mail.getContent(),
                mail.getProcessedAt(),
                mail.getCategory(),
                mail.getCategory() != null ? mail.getCategory().getDisplayName() : null,
                mail.getCategoryConfidence(),
                mail.getSummary(),
                mail.getAnalyzedAt()
        );
    }
}
