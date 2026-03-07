package com.example.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(
        urlPatterns = "/register",
        initParams = {
                @WebInitParam(name = "courseName", value = "Enterprise Java Course")
        }
)
public class StudentRegistrationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        out.println("<h2>Student Registration</h2>");
        out.println("<form method='post' action='register'>");

        out.println("Name: <input type='text' name='name'><br><br>");
        out.println("Email: <input type='email' name='email'><br><br>");

        out.println("<button type='submit'>Register</button>");

        out.println("</form>");
    }

    public void init() throws ServletException {

        getServletContext().setAttribute("studentCount", 0);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();


        String name = request.getParameter("name");
        String email = request.getParameter("email");

        Cookie userCookie = new Cookie("studentName", name);
        userCookie.setMaxAge(60*60*24);
        response.addCookie(userCookie);

        String courseName = getServletConfig().getInitParameter("courseName");

        ServletContext context = getServletContext();
        int count = (int) context.getAttribute("studentCount");
        count++;
        context.setAttribute("studentCount", count);

        out.println("<h2>Registration Successful!</h2>");
        out.println("<p>Welcome " + name + " to " + (courseName != null ? courseName : "Java Course") + "</p>");
        out.println("<p>Total Students Registered: " + count + "</p>");
    }
}
