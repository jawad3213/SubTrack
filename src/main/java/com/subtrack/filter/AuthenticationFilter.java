package com.subtrack.filter;

import com.subtrack.dao.ClientDAO;
import com.subtrack.entity.Client;
import com.subtrack.enums.Role;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;

/**
 * Server-side access control for every request: pages need a logged-in user,
 * and everything under /admin/ needs the ADMIN role. Hiding content with
 * rendered="..." in templates is only cosmetic; this filter is the real check.
 *
 * The user is re-read from the database on each request, so suspending an account
 * or changing its role takes effect immediately instead of at the next login.
 */
@WebFilter("/*")
public class AuthenticationFilter implements Filter {

    /** Session attribute written by UserContext.setCurrentUser. */
    private static final String SESSION_USER = "user";

    private static final Set<String> PUBLIC_PAGES = Set.of(
        "/", "/index.xhtml", "/login.xhtml", "/register.xhtml",
        "/forgot-password.xhtml", "/reset-password.xhtml"
    );

    @Inject
    private ClientDAO clientDAO;

    private static final String[] PUBLIC_PREFIXES = {
        "/error/", "/resources/", "/jakarta.faces.resource/"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (isPublic(path)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        Client sessionUser = session != null ? (Client) session.getAttribute(SESSION_USER) : null;
        Client user = sessionUser != null ? clientDAO.findById(sessionUser.getId()).orElse(null) : null;

        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            if (session != null && sessionUser != null) {
                session.invalidate(); // account deleted or suspended since login
            }
            redirect(request, response, request.getContextPath() + "/login.xhtml");
            return;
        }
        session.setAttribute(SESSION_USER, user);

        if (path.startsWith("/admin/") && user.getRole() != Role.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(req, res);
    }

    private boolean isPublic(String path) {
        if (PUBLIC_PAGES.contains(path)) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** JSF ajax requests can't follow an HTTP redirect, so they get a partial-response redirect instead. */
    private void redirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        if ("partial/ajax".equals(request.getHeader("Faces-Request"))) {
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            response.getWriter()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .printf("<partial-response><redirect url=\"%s\"></redirect></partial-response>", url);
        } else {
            response.sendRedirect(url);
        }
    }
}
