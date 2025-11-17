/**
 *  회원가입 JavaScript
 * - 실시간 폼 검증
 * - 비밀번호 강도 체크
 * - 사용자 경험 개선
 */

document.addEventListener('DOMContentLoaded', function() {
    initializeSignupForm();
});

/**
 * 회원가입 폼 초기화
 */
function initializeSignupForm() {
    const form = document.querySelector('.signup-form');
    if (!form) return;

    // 이벤트 리스너 등록
    setupFormValidation();
    setupPasswordToggle();
    setupPasswordStrengthCheck();
    setupFormSubmission();
}

/**
 * 폼 검증 설정
 */
function setupFormValidation() {
    const emailInput = document.getElementById('email');
    const loginPasswordInput = document.getElementById('loginPassword');
    const pop3PasswordInput = document.getElementById('pop3Password');

    // 이메일 실시간 검증
    if (emailInput) {
        emailInput.addEventListener('blur', validateEmail);
        emailInput.addEventListener('input', debounce(validateEmail, 500));
    }

    // 비밀번호 실시간 검증
    if (loginPasswordInput) {
        loginPasswordInput.addEventListener('input', function() {
            validatePassword(this, 'login');
        });
    }

    if (pop3PasswordInput) {
        pop3PasswordInput.addEventListener('input', function() {
            validatePassword(this, 'pop3');
        });
    }
}

/**
 * 이메일 검증
 */
function validateEmail() {
    const emailInput = document.getElementById('email');
    const emailValue = emailInput.value.trim();
    
    // 이메일 정규식
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    
    clearValidationMessage(emailInput);
    
    if (!emailValue) {
        showValidationError(emailInput, '이메일 주소를 입력해주세요.');
        return false;
    }
    
    if (!emailRegex.test(emailValue)) {
        showValidationError(emailInput, '올바른 이메일 형식이 아닙니다.');
        return false;
    }
    
    // 특정 도메인 제한 (예시)
    const allowedDomains = ['dsntech.com', 'gmail.com', 'naver.com', 'daum.net', 'hanmail.net'];
    const domain = emailValue.split('@')[1]?.toLowerCase();
    
    if (domain && !allowedDomains.includes(domain)) {
        showValidationWarning(emailInput, '일반적이지 않은 이메일 도메인입니다. 수신 가능한지 확인해주세요.');
    } else {
        showValidationSuccess(emailInput, '유효한 이메일 주소입니다.');
    }
    
    return true;
}

/**
 * 비밀번호 검증 및 강도 체크
 */
function validatePassword(passwordInput, type) {
    const passwordValue = passwordInput.value;
    
    clearValidationMessage(passwordInput);
    
    if (!passwordValue) {
        showValidationError(passwordInput, '비밀번호를 입력해주세요.');
        return false;
    }
    
    // POP3 비밀번호는 간단한 검증만 수행
    if (type === 'pop3') {
        if (passwordValue.length < 1) {
            showValidationError(passwordInput, 'POP3 비밀번호를 입력해주세요.');
            return false;
        }
        
        if (passwordValue.length > 255) {
            showValidationError(passwordInput, 'POP3 비밀번호는 255자를 초과할 수 없습니다.');
            return false;
        }
        
        showValidationSuccess(passwordInput, 'POP3 비밀번호가 입력되었습니다.');
        return true;
    }
    
    // 로그인 비밀번호는 복잡도 검증 수행
    if (passwordValue.length < 8) {
        showValidationError(passwordInput, '비밀번호는 최소 8자 이상이어야 합니다.');
        return false;
    }
    
    if (passwordValue.length > 64) {
        showValidationError(passwordInput, '비밀번호는 64자를 초과할 수 없습니다.');
        return false;
    }
    
    // 복잡도 검증 (로그인 비밀번호만)
    const hasLowerCase = /[a-z]/.test(passwordValue);
    const hasUpperCase = /[A-Z]/.test(passwordValue);
    const hasDigit = /\d/.test(passwordValue);
    const hasSpecialChar = /[@$!%*?&]/.test(passwordValue);
    
    const missing = [];
    if (!hasLowerCase) missing.push('소문자');
    if (!hasUpperCase) missing.push('대문자');
    if (!hasDigit) missing.push('숫자');
    if (!hasSpecialChar) missing.push('특수문자(@$!%*?&)');
    
    if (missing.length > 0) {
        showValidationError(passwordInput, `다음 조건이 부족합니다: ${missing.join(', ')}`);
        return false;
    }
    
    // 강도 체크 (로그인 비밀번호만)
    const strength = checkPasswordStrength(passwordValue);
    switch (strength) {
        case 'weak':
            showValidationWarning(passwordInput, '약한 비밀번호입니다. 더 복잡한 비밀번호를 권장합니다.');
            break;
        case 'medium':
            showValidationInfo(passwordInput, '보통 강도의 비밀번호입니다.');
            break;
        case 'strong':
            showValidationSuccess(passwordInput, '강한 비밀번호입니다.');
            break;
    }
    
    // 비밀번호 강도 표시기 업데이트 (로그인 비밀번호만)
    updatePasswordStrengthIndicator(passwordInput, strength);
    
    return true;
}

/**
 * 비밀번호 강도 체크
 */
function checkPasswordStrength(password) {
    let score = 0;
    
    // 길이 점수
    if (password.length >= 8) score += 1;
    if (password.length >= 12) score += 1;
    if (password.length >= 16) score += 1;
    
    // 복잡도 점수
    if (/[a-z]/.test(password)) score += 1;
    if (/[A-Z]/.test(password)) score += 1;
    if (/\d/.test(password)) score += 1;
    if (/[@$!%*?&]/.test(password)) score += 1;
    
    // 다양성 점수
    if (/[a-z].*[A-Z]|[A-Z].*[a-z]/.test(password)) score += 1;
    if (/\d.*[@$!%*?&]|[@$!%*?&].*\d/.test(password)) score += 1;
    
    // 일반적인 패턴 감점
    if (/^(?:password|123456|qwerty)/i.test(password)) score -= 3;
    if (/(.)\1{2,}/.test(password)) score -= 1; // 연속된 문자
    
    if (score < 4) return 'weak';
    if (score < 7) return 'medium';
    return 'strong';
}

/**
 * 비밀번호 강도 표시기 업데이트
 */
function updatePasswordStrengthIndicator(passwordInput, strength) {
    const container = passwordInput.closest('.mb-3');
    let indicator = container.querySelector('.password-strength-indicator');
    
    if (!indicator) {
        indicator = document.createElement('div');
        indicator.className = 'password-strength-indicator mt-2';
        container.appendChild(indicator);
    }
    
    const strengthColors = {
        weak: '#dc3545',
        medium: '#ffc107',
        strong: '#28a745'
    };
    
    const strengthTexts = {
        weak: '약함',
        medium: '보통',
        strong: '강함'
    };
    
    const strengthPercentages = {
        weak: 33,
        medium: 66,
        strong: 100
    };
    
    indicator.innerHTML = `
        <div class="d-flex justify-content-between align-items-center">
            <small class="text-muted">비밀번호 강도:</small>
            <small style="color: ${strengthColors[strength]}; font-weight: 600;">${strengthTexts[strength]}</small>
        </div>
        <div class="progress mt-1" style="height: 4px;">
            <div class="progress-bar" 
                 style="background-color: ${strengthColors[strength]}; width: ${strengthPercentages[strength]}%; transition: all 0.3s ease;"></div>
        </div>
    `;
}

/**
 * 비밀번호 토글 기능 설정
 */
function setupPasswordToggle() {
    document.querySelectorAll('.btn-password-toggle').forEach(button => {
        button.addEventListener('click', function() {
            const input = this.parentElement.querySelector('input');
            const icon = this.querySelector('i');
            
            if (input.type === 'password') {
                input.type = 'text';
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
                this.setAttribute('title', '비밀번호 숨기기');
            } else {
                input.type = 'password';
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
                this.setAttribute('title', '비밀번호 보기');
            }
        });
    });
}

/**
 * 비밀번호 강도 체크 설정
 */
function setupPasswordStrengthCheck() {
    const loginPassword = document.getElementById('loginPassword');
    const pop3Password = document.getElementById('pop3Password');
    
    // 비밀번호 일치 체크 (로그인/POP3 비밀번호가 같으면 경고) - POP3는 모든 문자열 허용하므로 경고만
    if (loginPassword && pop3Password) {
        function checkPasswordMatch() {
            if (loginPassword.value && pop3Password.value && 
                loginPassword.value === pop3Password.value) {
                showValidationWarning(pop3Password, '로그인 비밀번호와 POP3 비밀번호가 동일합니다. 보안을 위해 서로 다른 비밀번호를 권장합니다.');
            }
        }
        
        loginPassword.addEventListener('blur', checkPasswordMatch);
        pop3Password.addEventListener('blur', checkPasswordMatch);
    }
}

/**
 * 폼 제출 처리
 */
function setupFormSubmission() {
    const form = document.querySelector('.signup-form');
    if (!form) return;
    
    form.addEventListener('submit', function(e) {
        // 최종 검증
        const isValid = validateForm();
        
        if (!isValid) {
            e.preventDefault();
            showFormError('입력한 정보를 다시 확인해주세요.');
            return false;
        }
        
        // 제출 상태 표시
        setFormSubmitting(true);
        
        // 타임아웃 설정 (30초)
        setTimeout(() => {
            if (document.querySelector('.form-submitting')) {
                setFormSubmitting(false);
                showFormError('요청 처리 시간이 초과되었습니다. 다시 시도해주세요.');
            }
        }, 30000);
    });
}

/**
 * 전체 폼 검증
 */
function validateForm() {
    const emailValid = validateEmail();
    const loginPasswordValid = validatePassword(document.getElementById('loginPassword'), 'login');
    const pop3PasswordValid = validatePassword(document.getElementById('pop3Password'), 'pop3');
    const agreeTerms = document.getElementById('agreeTerms').checked;
    
    if (!agreeTerms) {
        showFormError('개인정보 처리방침 및 이용약관에 동의해주세요.');
        return false;
    }
    
    return emailValid && loginPasswordValid && pop3PasswordValid;
}

/**
 * 폼 제출 상태 설정
 */
function setFormSubmitting(isSubmitting) {
    const form = document.querySelector('.signup-form');
    const submitBtn = form.querySelector('.submit-btn');
    const submitIcon = submitBtn.querySelector('i');
    const submitText = submitBtn.querySelector('span') || submitBtn.childNodes[submitBtn.childNodes.length - 1];
    
    if (isSubmitting) {
        form.classList.add('form-submitting');
        submitIcon.className = 'fas fa-spinner-third fa-spin me-2';
        if (submitText.textContent) {
            submitText.textContent = ' 처리 중...';
        }
        submitBtn.disabled = true;
    } else {
        form.classList.remove('form-submitting');
        submitIcon.className = 'fas fa-paper-plane me-2';
        if (submitText.textContent) {
            submitText.textContent = ' 인증 코드 발송';
        }
        submitBtn.disabled = false;
    }
}

/**
 * 검증 메시지 표시 함수들
 */
function showValidationError(input, message) {
    input.classList.add('is-invalid');
    input.classList.remove('is-valid');
    showFeedback(input, message, 'invalid-feedback', 'text-danger');
}

function showValidationSuccess(input, message) {
    input.classList.add('is-valid');
    input.classList.remove('is-invalid');
    showFeedback(input, message, 'valid-feedback', 'text-success');
}

function showValidationWarning(input, message) {
    showFeedback(input, message, 'warning-feedback', 'text-warning');
}

function showValidationInfo(input, message) {
    showFeedback(input, message, 'info-feedback', 'text-info');
}

function showFeedback(input, message, className, textClass) {
    const container = input.closest('.mb-3');
    let feedback = container.querySelector(`.${className}`);
    
    if (!feedback) {
        feedback = document.createElement('div');
        feedback.className = `${className} ${textClass}`;
        container.appendChild(feedback);
    }
    
    feedback.textContent = message;
    feedback.style.display = 'block';
}

function clearValidationMessage(input) {
    input.classList.remove('is-invalid', 'is-valid');
    const container = input.closest('.mb-3');
    const feedbacks = container.querySelectorAll('.invalid-feedback, .valid-feedback, .warning-feedback, .info-feedback');
    feedbacks.forEach(feedback => {
        if (!feedback.hasAttribute('th:errors')) {
            feedback.remove();
        }
    });
}

function showFormError(message) {
    // 기존 에러 메시지 제거
    const existingError = document.querySelector('.form-error-alert');
    if (existingError) {
        existingError.remove();
    }
    
    // 새 에러 메시지 생성
    const errorAlert = document.createElement('div');
    errorAlert.className = 'alert alert-danger form-error-alert';
    errorAlert.innerHTML = `<i class="fas fa-exclamation-triangle me-2"></i>${message}`;
    
    // 폼 상단에 삽입
    const form = document.querySelector('.signup-form');
    form.insertBefore(errorAlert, form.firstChild);
    
    // 에러 메시지로 스크롤
    errorAlert.scrollIntoView({ behavior: 'smooth', block: 'center' });
    
    // 3초 후 자동 제거
    setTimeout(() => {
        errorAlert.remove();
    }, 5000);
}

/**
 * 유틸리티 함수들
 */

// 디바운스 함수
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func.apply(this, args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// 비밀번호 토글 함수 (전역으로 노출)
window.togglePassword = function(inputId) {
    const input = document.getElementById(inputId);
    const button = input.parentElement.querySelector('.btn-password-toggle');
    const icon = button.querySelector('i');
    
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
    }
};

// 접근성 개선
document.addEventListener('keydown', function(e) {
    // ESC 키로 에러 메시지 닫기
    if (e.key === 'Escape') {
        const errorAlert = document.querySelector('.form-error-alert');
        if (errorAlert) {
            errorAlert.remove();
        }
    }
});

// 폼 자동 저장 (브라우저 재시작 시 복원)
function setupFormAutoSave() {
    const form = document.querySelector('.signup-form');
    if (!form) return;
    
    // 이메일만 저장 (비밀번호는 보안상 저장하지 않음)
    const emailInput = document.getElementById('email');
    
    if (emailInput) {
        // 저장된 이메일 복원
        const savedEmail = localStorage.getItem('signup_email');
        if (savedEmail) {
            emailInput.value = savedEmail;
        }
        
        // 이메일 입력 시 저장
        emailInput.addEventListener('input', debounce(function() {
            localStorage.setItem('signup_email', this.value);
        }, 1000));
        
        // 폼 제출 시 저장된 데이터 제거
        form.addEventListener('submit', function() {
            localStorage.removeItem('signup_email');
        });
    }
}

// 페이지 로드 시 자동 저장 설정 실행
document.addEventListener('DOMContentLoaded', setupFormAutoSave);