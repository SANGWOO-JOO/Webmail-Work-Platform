package dsn.webmail.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class AuthWebController {

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String loginPage(Model model,
                           @ModelAttribute("successMessage") String successMessage) {
        if (successMessage != null && !successMessage.isEmpty()) {
            model.addAttribute("successMessage", successMessage);
        }
        return "login";
    }

    /**
     * 로그아웃 처리
     * JWT는 stateless이므로 서버에서 할 작업은 없음
     * 클라이언트에서 LocalStorage 토큰 삭제 후 로그인 페이지로 리다이렉트
     */
    @GetMapping("/logout")
    public String logout() {
        // JWT는 stateless이므로 서버에서 세션을 제거할 필요 없음
        // 클라이언트 측에서 LocalStorage의 토큰 제거
        return "redirect:/login";
    }
}
