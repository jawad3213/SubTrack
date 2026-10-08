/* =====================================================================
   Landing page animations (index.xhtml). Requires GSAP + ScrollTrigger.
   The page is fully readable without this script; it only adds motion.
   ===================================================================== */
(function () {
    'use strict';

    var nav = document.getElementById('nav');
    function updateNav() {
        if (nav) nav.classList.toggle('scrolled', window.scrollY > 10);
    }
    updateNav();
    window.addEventListener('scroll', updateNav, { passive: true });

    if (!window.gsap) return;
    gsap.registerPlugin(ScrollTrigger);

    function formatNumber(value, decimals) {
        return value.toLocaleString('en-US', {
            minimumFractionDigits: decimals,
            maximumFractionDigits: decimals
        });
    }

    /** Counts a .count element from 0 to its data-to value. */
    function countUp(el, delay) {
        var target = parseFloat(el.getAttribute('data-to')) || 0;
        var decimals = parseInt(el.getAttribute('data-decimals'), 10) || 0;
        var state = { v: 0 };
        el.textContent = formatNumber(0, decimals);
        return gsap.to(state, {
            v: target,
            duration: 1.6,
            delay: delay || 0,
            ease: 'power2.out',
            onUpdate: function () { el.textContent = formatNumber(state.v, decimals); }
        });
    }

    // Duplicate the marquee content so the loop is seamless at any width.
    var track = document.querySelector('.marquee-track');
    if (track) track.innerHTML += track.innerHTML;

    var mm = gsap.matchMedia();

    mm.add('(prefers-reduced-motion: no-preference)', function () {

        /* ---------- Hero intro ---------- */
        var intro = gsap.timeline({ defaults: { ease: 'power3.out' } });
        intro
            .from('.nav-inner > *', { y: -16, autoAlpha: 0, duration: 0.6, stagger: 0.08 })
            .from('.hero .eyebrow', { y: 16, autoAlpha: 0, duration: 0.6 }, '-=0.3')
            .from('.hero-title .line-inner', { yPercent: 110, duration: 1, stagger: 0.12, ease: 'power4.out' }, '-=0.35')
            .from('.hero-lead, .hero-ctas, .hero-points', { y: 20, autoAlpha: 0, duration: 0.7, stagger: 0.1 }, '-=0.6')
            .from('.mock', {
                y: 60, rotateX: 14, rotateY: -10, autoAlpha: 0, scale: 0.94,
                duration: 1.3, ease: 'expo.out'
            }, 0.35)
            .from('.mock-kpi', { y: 14, autoAlpha: 0, duration: 0.5, stagger: 0.08 }, '-=0.8')
            .from('.mock-bars i', { scaleY: 0, duration: 0.8, stagger: 0.06, ease: 'back.out(1.6)' }, '-=0.5')
            .from('.mock-row', { x: -16, autoAlpha: 0, duration: 0.5, stagger: 0.08 }, '-=0.6')
            .from('.float-card', { y: 24, scale: 0.9, autoAlpha: 0, duration: 0.7, stagger: 0.18, ease: 'back.out(1.7)' }, '-=0.4');

        document.querySelectorAll('.mock .count').forEach(function (el, i) {
            intro.add(countUp(el), 0.9 + i * 0.1);
        });

        // Gentle idle float on the notification cards, once they have appeared.
        intro.add(function () {
            gsap.utils.toArray('.float-card').forEach(function (card, i) {
                gsap.to(card, {
                    y: i % 2 ? 10 : -10,
                    duration: 2.6 + i * 0.4,
                    ease: 'sine.inOut',
                    repeat: -1,
                    yoyo: true
                });
            });
        });

        // Mockup tilts slightly toward the cursor.
        var visual = document.querySelector('.hero-visual');
        var mock = document.querySelector('.mock');
        if (visual && mock && window.matchMedia('(pointer: fine)').matches) {
            var rotX = gsap.quickTo(mock, 'rotateX', { duration: 0.8, ease: 'power3.out' });
            var rotY = gsap.quickTo(mock, 'rotateY', { duration: 0.8, ease: 'power3.out' });
            visual.addEventListener('mousemove', function (e) {
                var r = visual.getBoundingClientRect();
                rotY(((e.clientX - r.left) / r.width - 0.5) * 8);
                rotX(-((e.clientY - r.top) / r.height - 0.5) * 8);
            });
            visual.addEventListener('mouseleave', function () { rotX(0); rotY(0); });
        }

        // Hero drifts away on scroll.
        gsap.to('.hero-glow', {
            yPercent: 30,
            ease: 'none',
            scrollTrigger: { trigger: '.hero', start: 'top top', end: 'bottom top', scrub: true }
        });
        gsap.to('.hero-visual', {
            y: -60,
            ease: 'none',
            scrollTrigger: { trigger: '.hero', start: 'top top', end: 'bottom top', scrub: true }
        });

        /* ---------- Marquee ---------- */
        if (track) {
            var loop = gsap.to(track, { xPercent: -50, duration: 40, ease: 'none', repeat: -1 });
            track.addEventListener('mouseenter', function () { gsap.to(loop, { timeScale: 0.2, duration: 0.6 }); });
            track.addEventListener('mouseleave', function () { gsap.to(loop, { timeScale: 1, duration: 0.6 }); });
        }

        /* ---------- Section reveals ---------- */
        gsap.utils.toArray('.reveal').forEach(function (el) {
            gsap.from(el, {
                y: 40, autoAlpha: 0, duration: 0.9, ease: 'power3.out',
                scrollTrigger: { trigger: el, start: 'top 85%' }
            });
        });

        gsap.set('.reveal-card', { autoAlpha: 0 });
        ScrollTrigger.batch('.reveal-card', {
            start: 'top 88%',
            onEnter: function (batch) {
                gsap.fromTo(batch,
                    { y: 40, autoAlpha: 0 },
                    { y: 0, autoAlpha: 1, duration: 0.8, stagger: 0.1, ease: 'power3.out', overwrite: true });
            }
        });

        /* ---------- The idea: problem → solution ---------- */
        gsap.from('.idea-arrow', {
            scale: 0, rotate: -90, duration: 0.8, ease: 'back.out(2)',
            scrollTrigger: { trigger: '.idea-grid', start: 'top 75%' }
        });
        gsap.from('.idea-card li', {
            x: -14, autoAlpha: 0, duration: 0.5, stagger: 0.06, ease: 'power2.out',
            scrollTrigger: { trigger: '.idea-grid', start: 'top 70%' }
        });

        /* ---------- Features: icons pop in ---------- */
        gsap.from('.feature-ico', {
            scale: 0.4, rotate: -20, duration: 0.7, stagger: 0.08, ease: 'back.out(2.2)',
            scrollTrigger: { trigger: '.feature-grid', start: 'top 80%' }
        });

        /* ---------- How it works: line draws as you scroll ---------- */
        gsap.from('.steps-line-fill', {
            scaleX: 0, ease: 'none',
            scrollTrigger: { trigger: '.steps', start: 'top 75%', end: 'bottom 60%', scrub: 0.6 }
        });
        gsap.utils.toArray('.step').forEach(function (step, i) {
            var tl = gsap.timeline({ scrollTrigger: { trigger: '.steps', start: 'top ' + (75 - i * 8) + '%' } });
            tl.from(step.querySelector('.step-num'), { scale: 0, duration: 0.6, ease: 'back.out(2)' })
              .from(step.querySelectorAll('h3, p'), { y: 20, autoAlpha: 0, duration: 0.6, stagger: 0.08, ease: 'power3.out' }, '-=0.3');
        });

        /* ---------- Benefits ---------- */
        gsap.from('.benefit', {
            x: -30, autoAlpha: 0, duration: 0.7, stagger: 0.12, ease: 'power3.out',
            scrollTrigger: { trigger: '.benefit-list', start: 'top 80%' }
        });

        var savings = gsap.timeline({ scrollTrigger: { trigger: '.savings-card', start: 'top 78%' } });
        savings
            .from('.savings-card', { y: 60, rotate: 2, autoAlpha: 0, duration: 1, ease: 'expo.out' })
            .from('.meter i', { scaleX: 0, duration: 1, stagger: 0.15, ease: 'power3.out' }, '-=0.5');
        var total = document.querySelector('.savings-total .count');
        if (total) savings.add(countUp(total), 0.3);

        /* ---------- CTA ---------- */
        gsap.from('.cta-card', {
            y: 60, scale: 0.96, autoAlpha: 0, duration: 1, ease: 'expo.out',
            scrollTrigger: { trigger: '.cta', start: 'top 85%' }
        });
    });
})();
