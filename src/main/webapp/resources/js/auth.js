/* =====================================================================
   Sign in / sign up behaviour (login.xhtml, register.xhtml).
   Form helpers work without GSAP; GSAP only adds the entrance motion.
   ===================================================================== */
(function () {
    'use strict';

    /* ---------- Show / hide password ---------- */
    document.querySelectorAll('.pw-toggle').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var input = document.getElementById(btn.getAttribute('data-target'));
            if (!input) return;
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            btn.classList.toggle('on', show);
            btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
            input.focus();
        });
    });

    /* ---------- Account type picker (register) ---------- */
    var typeInput = document.getElementById('accountType');
    var typeOpts = document.querySelectorAll('.type-opt');
    function selectType(value) {
        typeOpts.forEach(function (opt) {
            var on = opt.getAttribute('data-type') === value;
            opt.classList.toggle('active', on);
            opt.setAttribute('aria-checked', on ? 'true' : 'false');
        });
        if (typeInput) typeInput.value = value;
    }
    if (typeInput && typeOpts.length) {
        selectType(typeInput.value || 'B2C');
        typeOpts.forEach(function (opt) {
            opt.addEventListener('click', function () {
                selectType(opt.getAttribute('data-type'));
                if (window.gsap) gsap.fromTo(opt, { scale: 0.95 }, { scale: 1, duration: 0.4, ease: 'back.out(3)' });
            });
        });
    }

    /* ---------- Password strength (register) ---------- */
    // Same rules as PasswordUtil.isPasswordStrong on the server.
    var rules = {
        length: function (v) { return v.length >= 8; },
        upper:  function (v) { return /[A-Z]/.test(v); },
        lower:  function (v) { return /[a-z]/.test(v); },
        digit:  function (v) { return /[0-9]/.test(v); }
    };
    var meter = document.getElementById('pwMeter');
    var ruleList = document.getElementById('pwRules');
    var pw = document.getElementById('password');
    var confirmPw = document.getElementById('confirmPassword');
    var matchHint = document.getElementById('matchHint');

    function updateStrength() {
        if (!meter || !pw) return;
        var value = pw.value;
        var score = 0;
        ruleList.querySelectorAll('li').forEach(function (li) {
            var ok = rules[li.getAttribute('data-rule')](value);
            li.classList.toggle('ok', ok);
            if (ok) score++;
        });
        meter.setAttribute('data-score', value ? String(score) : '0');
    }
    function updateMatch() {
        if (!matchHint || !confirmPw) return;
        if (!confirmPw.value) {
            matchHint.textContent = '';
            matchHint.className = 'match-hint';
            return;
        }
        var same = confirmPw.value === pw.value;
        matchHint.textContent = same ? 'Passwords match' : 'Passwords do not match yet';
        matchHint.className = 'match-hint ' + (same ? 'ok' : 'bad');
    }
    if (meter && pw) {
        pw.addEventListener('input', function () { updateStrength(); updateMatch(); });
        updateStrength();
    }
    if (confirmPw) confirmPw.addEventListener('input', updateMatch);

    /* ---------- Submit loading state ---------- */
    // The button is never disabled: JSF needs its name in the request to run the action.
    document.querySelectorAll('.auth-form').forEach(function (form) {
        var submitting = false;
        form.addEventListener('submit', function (e) {
            if (submitting) { e.preventDefault(); return; }
            submitting = true;
            var wrap = form.querySelector('.submit-wrap');
            if (wrap) wrap.classList.add('loading');
        });
        // Reset if the page is restored from the back/forward cache.
        window.addEventListener('pageshow', function () {
            submitting = false;
            var wrap = form.querySelector('.submit-wrap');
            if (wrap) wrap.classList.remove('loading');
        });
    });

    /* ---------- Motion ---------- */
    if (!window.gsap) return;

    function countUp(el) {
        var target = parseFloat(el.getAttribute('data-to')) || 0;
        var state = { v: 0 };
        return gsap.to(state, {
            v: target,
            duration: 1.6,
            ease: 'power2.out',
            onUpdate: function () { el.textContent = Math.round(state.v).toLocaleString('en-US'); }
        });
    }

    gsap.matchMedia().add('(prefers-reduced-motion: no-preference)', function () {
        var hasError = document.querySelector('.field-error, .auth-messages li');

        // Form panel: quick and subtle, and skipped after a failed submit so errors show at once.
        if (!hasError) {
            gsap.timeline({ defaults: { ease: 'power3.out' } })
                .from('.auth-top > *', { y: -12, autoAlpha: 0, duration: 0.5, stagger: 0.08 })
                .from('.auth-form-wrap .anim', { y: 18, autoAlpha: 0, duration: 0.6, stagger: 0.06 }, '-=0.25')
                .from('.auth-bottom', { autoAlpha: 0, duration: 0.5 }, '-=0.3');
        } else {
            gsap.from('.auth-messages li, .field-error', { x: -8, duration: 0.5, ease: 'elastic.out(1, 0.4)' });
        }

        // Brand panel (hidden on small screens).
        var panel = document.querySelector('.brand-panel');
        if (!panel || getComputedStyle(panel).display === 'none') return;

        var tl = gsap.timeline({ defaults: { ease: 'power3.out' }, delay: 0.15 });
        tl.from('.bp-glow', { scale: 0.6, autoAlpha: 0, duration: 1.6, ease: 'power2.out' }, 0)
          .from('.bp-anim', { y: 24, autoAlpha: 0, duration: 0.8, stagger: 0.1 }, 0.1)
          .from('.bp-step', { x: -20, autoAlpha: 0, duration: 0.6, stagger: 0.12 }, 0.45)
          .from('.bp-card', { y: 50, rotate: -2, scale: 0.94, autoAlpha: 0, duration: 1.1, ease: 'expo.out' }, 0.45)
          .from('.bp-bars i', { scaleY: 0, duration: 0.7, stagger: 0.05, ease: 'back.out(1.6)' }, 0.85)
          .from('.bp-meter i', { scaleX: 0, duration: 1, ease: 'power3.out' }, 0.9)
          .from('.bp-row', { x: -12, autoAlpha: 0, duration: 0.5, stagger: 0.08 }, 1)
          .from('.bp-float', { y: 20, scale: 0.85, autoAlpha: 0, duration: 0.7, stagger: 0.18, ease: 'back.out(1.8)' }, 1.05);

        document.querySelectorAll('.bp-card .count').forEach(function (el) { tl.add(countUp(el), 0.8); });

        tl.add(function () {
            gsap.utils.toArray('.bp-float').forEach(function (card, i) {
                gsap.to(card, { y: i % 2 ? 8 : -8, duration: 2.6 + i * 0.5, ease: 'sine.inOut', repeat: -1, yoyo: true });
            });
            gsap.to('.bp-glow', { x: -40, y: 30, duration: 8, ease: 'sine.inOut', repeat: -1, yoyo: true });
        });

        // Card follows the pointer slightly.
        var stage = panel.querySelector('.bp-stage');
        if (stage && window.matchMedia('(pointer: fine)').matches) {
            var mx = gsap.quickTo(stage, 'x', { duration: 0.9, ease: 'power3.out' });
            var my = gsap.quickTo(stage, 'y', { duration: 0.9, ease: 'power3.out' });
            panel.addEventListener('mousemove', function (e) {
                var r = panel.getBoundingClientRect();
                mx(((e.clientX - r.left) / r.width - 0.5) * 16);
                my(((e.clientY - r.top) / r.height - 0.5) * 16);
            });
            panel.addEventListener('mouseleave', function () { mx(0); my(0); });
        }
    });
})();
