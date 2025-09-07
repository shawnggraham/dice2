package com.example.dice;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiceServlet extends HttpServlet {
    private static final Random RNG = new Random();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handle(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handle(req, resp);
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=utf-8");
        String notation = t(req.getParameter("notation"));
        String ds = t(req.getParameter("dice"));
        String ss = t(req.getParameter("sides"));

        Integer d = null, s = null;
        if (notation != null && !notation.isEmpty()) {
            Matcher m = Pattern.compile("^(\\d+)[dD](\\d+)$").matcher(notation);
            if (!m.matches()) { err(resp, 400, "Use NdS (e.g., 2d6) or provide dice and sides"); return; }
            d = p(m.group(1)); s = p(m.group(2));
        } else if (ds != null && ss != null) {
            d = p(ds); s = p(ss);
        } else { err(resp, 400, "Missing parameters"); return; }

        if (d == null || s == null) { err(resp, 400, "Non-integer input"); return; }
        if (d < 1 || d > 100000) { err(resp, 400, "dice must be 1..100000"); return; }
        if (s < 2 || s > 1000000) { err(resp, 400, "sides must be 2..1000000"); return; }

        int[] rolls = new int[d];
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0;
        for (int i = 0; i < d; i++) {
            int v = 1 + RNG.nextInt(s);
            rolls[i] = v; sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double avg = sum / (double) d;
        int[] sorted = rolls.clone(); Arrays.sort(sorted);
        double median = (d % 2 == 1) ? sorted[d / 2] : (sorted[d / 2 - 1] + sorted[d / 2]) / 2.0;

        StringBuilder sb = new StringBuilder(64 + d * 3);
        sb.append('{')
                .append("\"dice\":").append(d).append(',')
                .append("\"sides\":").append(s).append(',')
                .append("\"notation\":\"").append(d).append('d').append(s).append("\",")
                .append("\"results\":[");
        for (int i = 0; i < rolls.length; i++) { if (i > 0) sb.append(','); sb.append(rolls[i]); }
        sb.append("],")
                .append("\"total\":").append(sum).append(',')
                .append("\"min\":").append(min).append(',')
                .append("\"max\":").append(max).append(',')
                .append("\"average\":").append(trim(avg)).append(',')
                .append("\"median\":").append(trim(median))
                .append('}');
        resp.getWriter().write(sb.toString());
    }

    private static String t(String s){ return s==null?null:s.trim(); }
    private static Integer p(String s){ try { return Integer.parseInt(s.trim()); } catch(Exception e){ return null; } }
    private static String trim(double d){ String s=String.valueOf(d); if (s.contains(".")) s=s.replaceAll("0+$","").replaceAll("\\.$",""); return s; }
    private static void err(HttpServletResponse r,int c,String m) throws IOException { r.setStatus(c); r.getWriter().write("{\"error\":\""+m.replace("\"","\\\"")+"\"}"); }
}
