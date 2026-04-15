package com.subtrack.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/oauth/callback")
public class OAuthCallbackServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String error = request.getParameter("error");
        
        if (error != null) {
            response.sendRedirect(request.getContextPath() + "/settings.xhtml?error=" + error);
            return;
        }
        
        request.setAttribute("code", code);
        request.setAttribute("state", state);
        request.getRequestDispatcher("/oauth/callback.xhtml").forward(request, response);
    }
}