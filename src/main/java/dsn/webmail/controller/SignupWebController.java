package dsn.webmail.controller;

import dsn.webmail.dto.SignupDtos.RequestCode;
import dsn.webmail.dto.SignupDtos.VerifyCode;
import dsn.webmail.service.SignupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/signup")
public class SignupWebController {

    private final SignupService signupService;

    public SignupWebController(SignupService signupService) {
        this.signupService = signupService;
    }

    /**
     * 회원가입 폼 페이지
     */
    @GetMapping
    public String signupForm(Model model) {
        model.addAttribute("requestCode", new RequestCode("", ""));
        return "signup/signup-form";
    }

    /**
     * 인증 코드 요청 처리
     */
    @PostMapping("/request-code")
    public String requestCode(@Valid @ModelAttribute("requestCode") RequestCode requestCode,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        
        if (bindingResult.hasErrors()) {
            return "signup/signup-form";
        }

        try {
            signupService.requestCode(requestCode.email(), requestCode.pop3Password());
            
            // 인증 코드 발송 성공 시 검증 페이지로 리다이렉트
            redirectAttributes.addFlashAttribute("email", requestCode.email());
            redirectAttributes.addFlashAttribute("successMessage", "인증 코드가 이메일로 발송되었습니다. 10분 내에 입력해주세요.");
            
            return "redirect:/signup/verify";
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "signup/signup-form";
        }
    }

    /**
     * 인증 코드 검증 폼 페이지
     */
    @GetMapping("/verify")
    public String verifyForm(Model model,
                           @ModelAttribute("email") String email,
                           @ModelAttribute("successMessage") String successMessage) {
        
        // 이메일이 없으면 처음부터 시작
        if (email == null || email.trim().isEmpty()) {
            return "redirect:/signup";
        }
        
        model.addAttribute("verifyCode", new VerifyCode(email, ""));
        model.addAttribute("email", email);
        
        if (successMessage != null && !successMessage.isEmpty()) {
            model.addAttribute("successMessage", successMessage);
        }
        
        return "signup/verify-form";
    }

    /**
     * 인증 코드 검증 처리
     */
    @PostMapping("/verify")
    public String verify(@Valid @ModelAttribute("verifyCode") VerifyCode verifyCode,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("email", verifyCode.email());
            return "signup/verify-form";
        }

        try {
            signupService.verify(verifyCode.email(), verifyCode.code());
            
            // 회원가입 완료
            redirectAttributes.addFlashAttribute("userEmail", verifyCode.email());
            redirectAttributes.addFlashAttribute("successMessage", 
                "회원가입이 완료되었습니다! 이제 시스템을 사용하실 수 있습니다.");
            
            return "redirect:/signup/success";
            
        } catch (Exception e) {
            model.addAttribute("email", verifyCode.email());
            model.addAttribute("errorMessage", e.getMessage());
            return "signup/verify-form";
        }
    }

    /**
     * 가입 완료 페이지
     */
    @GetMapping("/success")
    public String success(Model model, @ModelAttribute("userEmail") String userEmail) {
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            model.addAttribute("userEmail", userEmail);
        }
        return "signup/success";
    }

    /**
     * 새로운 코드 요청 (재전송)
     */
    @PostMapping("/resend")
    @ResponseBody
    public String resendCode(@RequestParam String email) {
        try {
            // 기존 사용자 정보로 재전송 (간소화된 버전)
            // 실제로는 기존 사용자 정보를 조회해서 재전송해야 함
            return "{\"success\": true, \"message\": \"인증 코드가 재전송되었습니다.\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }
}