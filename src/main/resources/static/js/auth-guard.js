/**
 * Auth Guard - JWT 인증 보호 및 자동 갱신
 * 대시보드 등 보호된 페이지에서 사용
 */

(function() {
    'use strict';

    // 현재 페이지가 보호되어야 하는지 확인
    const isProtectedPage = window.location.pathname.startsWith('/dashboard');

    if (!isProtectedPage) {
        return; // 보호되지 않은 페이지는 체크하지 않음
    }

    // 토큰 확인
    const accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');

    if (!accessToken) {
        console.log('[Auth Guard] No access token found, redirecting to login');
        redirectToLogin();
        return;
    }

    // JWT 토큰 유효성 검증
    try {
        const payload = JSON.parse(atob(accessToken.split('.')[1]));
        const exp = payload.exp * 1000; // 초 → 밀리초
        const now = Date.now();

        console.log('[Auth Guard] Token expires at:', new Date(exp));
        console.log('[Auth Guard] Current time:', new Date(now));

        // 토큰이 5분 이내에 만료될 경우 자동 갱신
        const REFRESH_THRESHOLD = 5 * 60 * 1000; // 5분
        if (exp - now < REFRESH_THRESHOLD) {
            console.log('[Auth Guard] Token will expire soon, attempting refresh');
            refreshAccessToken();
        } else if (now >= exp) {
            console.log('[Auth Guard] Token has expired, attempting refresh');
            refreshAccessToken();
        } else {
            console.log('[Auth Guard] Token is valid');
        }
    } catch (error) {
        console.error('[Auth Guard] Invalid token format:', error);
        redirectToLogin();
    }

    /**
     * Access Token 갱신
     */
    async function refreshAccessToken() {
        const refreshToken = localStorage.getItem('refreshToken');

        if (!refreshToken) {
            console.log('[Auth Guard] No refresh token, redirecting to login');
            redirectToLogin();
            return;
        }

        try {
            console.log('[Auth Guard] Refreshing access token...');
            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });

            if (response.ok) {
                const data = await response.json();
                localStorage.setItem('accessToken', data.accessToken);
                console.log('[Auth Guard] Access token refreshed successfully');
            } else {
                console.error('[Auth Guard] Refresh failed:', response.status);
                localStorage.clear();
                redirectToLogin();
            }
        } catch (error) {
            console.error('[Auth Guard] Refresh request failed:', error);
            localStorage.clear();
            redirectToLogin();
        }
    }

    /**
     * 로그인 페이지로 리다이렉트
     */
    function redirectToLogin() {
        const currentPath = window.location.pathname;
        const returnUrl = encodeURIComponent(currentPath);
        window.location.href = `/login?returnUrl=${returnUrl}`;
    }

    /**
     * API 요청 헬퍼 (Authorization 헤더 자동 추가)
     */
    window.apiRequest = async function(url, options = {}) {
        const accessToken = localStorage.getItem('accessToken');

        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (accessToken) {
            headers['Authorization'] = `Bearer ${accessToken}`;
        }

        let response = await fetch(url, {
            ...options,
            headers
        });

        // 401 에러 시 토큰 갱신 후 재시도
        if (response.status === 401) {
            console.log('[API Request] 401 Unauthorized, attempting token refresh');
            await refreshAccessToken();

            // 갱신된 토큰으로 재시도
            const newAccessToken = localStorage.getItem('accessToken');
            if (newAccessToken) {
                headers['Authorization'] = `Bearer ${newAccessToken}`;
                response = await fetch(url, { ...options, headers });
            } else {
                redirectToLogin();
                throw new Error('Authentication failed');
            }
        }

        return response;
    };

    console.log('[Auth Guard] Initialized successfully');
})();
