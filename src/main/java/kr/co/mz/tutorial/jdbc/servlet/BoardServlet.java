package kr.co.mz.tutorial.jdbc.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import kr.co.mz.tutorial.jdbc.Constants;
import kr.co.mz.tutorial.jdbc.db.model.Board;
import kr.co.mz.tutorial.jdbc.exception.DatabaseAccessException;
import kr.co.mz.tutorial.jdbc.exception.InputValidationException;
import kr.co.mz.tutorial.jdbc.service.BoardService;

public class BoardServlet extends HttpServlet {

    private DataSource dataSource;

    @Override
    public void init() {
        this.dataSource = (DataSource) getServletContext().getAttribute(Constants.DATASOURCE_CONTEXT_KEY);
    }

    private static final String PAGE_CONTENTS = """
        <!DOCTYPE html>
        <html>
        <head>
        <title>게시판</title>
        <style>
        /* CSS 코드 */
        h1 {
          color: white;
          font-family: 'Montserrat', sans-serif;
          background-color: #f90;
          text-align: center;
          font-size: 36px;
          font-weight: bold;
          height: 50px;
        }

        .category {
          margin-bottom: 20px;
        }

        #post-table {
          width: 100%;
          border-collapse: collapse;
        }

        #post-table th,
        #post-table td {
          padding: 8px;
          border: 1px solid #ccc;
        }

        .write-post {
          margin-top: 20px;
          text-align: right;
        }

        .write-post a {
          padding: 8px 16px;
          background-color: #f90;
          color: #fff;
          text-decoration: none;
          border-radius: 4px;
        }

        tr, td {
          text-align: center;
        }
        .pagination {
            display: flex;
            list-style: none;
            padding: 0;
            overflow-x: auto;
            white-space: nowrap;
            justify-content: center;
          }
                
          .pagination li {
            margin-right: 5px;
          }
                
          .pagination li a {
            display: inline-block;
            padding: 5px 10px;
            text-decoration: none;
            color: #333;
            border: 1px solid #ccc;
            border-radius: 4px;
          }
                
          .pagination li a.active {
            font-weight: bold;
            background-color: #f90;
            color: #fff;
          }
        // 추가적인 CSS 코드 작성
        </style>
        <script>
        // JavaScript 코드
        document.addEventListener('DOMContentLoaded', function() {
          // DOM이 로드된 후 실행될 JavaScript 코드
          // 카테고리 선택 드롭다운 메뉴의 이벤트 처리 로직
          var categoryDropdown = document.getElementById('category-dropdown');
          categoryDropdown.addEventListener('change', function() {
            var selectedCategory = categoryDropdown.value;
            console.log('선택한 카테고리:', selectedCategory);
            // 선택한 카테고리에 따라 동적으로 페이지를 로드하거나 필터링하는 로직을 추가하세요.
          });
        });
        </script>
        </head>
        <body>
        <h1>게 시 판</h1>
        <form action="/board" method="get" accept-charset="UTF-8">
        <div class="category">
          <label for="category-dropdown">카테고리:</label>
          <select id="category-dropdown" name="category">
        """;


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var blockNum = req.getParameter("blockNum") == null ? 1 : Integer.parseInt(req.getParameter("blockNum"));
        var pageNum = req.getParameter("pageNum") == null ? 1 : Integer.parseInt(req.getParameter("pageNum"));
        var category = req.getParameter("category") == null ? "전체" : req.getParameter("category");
        validateInputParameter(category);
        List<Board> boardList;
        try (var connection = dataSource.getConnection()) {
            boardList = new BoardService(connection).getByCategory(category);
        } catch (SQLException sqle) {
            throw new DatabaseAccessException(sqle);
        }
        var maxBoardCountInOnePage = 10;
        // 1 > 1~10         2> 11~20        3> 21~30
        // pageNum(boardCount)-(boardCount-1) ~ pageNum(boardCount)
        double totalPageCount = 0;
        var maxPageCount = 5;
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        out.println(PAGE_CONTENTS);
        out.println("    <option value=\"전체\" " + (category.equals("전체") ? " selected" : "")
            + ">전체</option>");
        out.println(
            "    <option value=\"여행 경험 공유\" " + (category.equals("여행 경험 공유") ? " selected" : "")
                + ">여행 경험 공유</option>");
        out.println("    <option value=\"여행지 추천\" " + (category.equals("여행지 추천") ? " selected" : "")
            + ">여행지 추천</option>");
        out.println(
            "    <option value=\"여행 계획 토론\" " + (category.equals("여행 계획 토론") ? " selected" : "")
                + ">여행 계획 토론</option>");
        out.println("  </select>");
        out.println("<input type=\"submit\" style=\"background-color: #f90; color:white; border:none\" value=\"확인\"/>");
        out.println("</div>");
        out.println("</form>");
        out.println("<table id=\"post-table\">");
        out.println("  <thead>");
        out.println("  <tr>");
        out.println("    <th>글 번호</th>");
        out.println("    <th>제목</th>");
        out.println("    <th>작성자</th>");
        out.println("    <th>작성일</th>");
        out.println("  </tr>");
        out.println("  </thead>");
        out.println("  <tbody>");
        var boardSize = 0;
        if (boardList != null && !boardList.isEmpty()) {
            boardSize = boardList.size();
            var index = 1;
            var startIndex = pageNum * maxBoardCountInOnePage - (maxBoardCountInOnePage - 1);
            var endIndex = Math.min(pageNum * maxBoardCountInOnePage, boardList.size());
            for (Board board : boardList) {
                if (startIndex <= index && endIndex >= index) {
                    out.println("  <tr>");
                    out.println("    <td>" + (index) + "</td>");
                    out.println("    <td><a href=\"/board/view?boardSeq=" + board.getSeq() + "\">" + board.getTitle()
                        + "</a></td>"); // href 닫는 따옴표 추가
                    out.println("    <td>" + board.getCustomerName() + "</td>");
                    out.println("    <td>" + board.getModifiedTime() + "</td>");
                    out.println("  </tr>");
                }
                index++;
            }
            totalPageCount = Math.ceil((double) boardList.size() / maxBoardCountInOnePage);
        }
        out.println("  </tbody>");
        out.println("</table>");
        out.println("<ul class=\"pagination\">");
        if (blockNum > 1) {
            out.println(
                "<li><a href=\"/board?blockNum=" + (blockNum - 1) + "&&pageNum=" + ((blockNum - 2) * maxPageCount + 1)
                    + "&&category=" + category + "\">←</a></li>");
        }
        var maxPageNumInBlock = totalPageCount > maxPageCount * blockNum ? maxPageCount * blockNum : totalPageCount;
        for (int i = (blockNum - 1) * maxPageCount; i < maxPageNumInBlock; i++) {
            out.println(
                "<li><a " + ((pageNum == i + 1) ? "style=\"background-color: #f90\"" : "") + " href=\"/board?blockNum="
                    + blockNum + "&&pageNum=" + (i + 1) + "&&category=" + category + "\">" + (i + 1) + "</a></li>");
        }

        if (blockNum * maxPageCount * maxBoardCountInOnePage < boardSize) {
            out.println("<li><a href=\"/board?blockNum=" + (blockNum + 1) + "&&pageNum=" + (blockNum * maxPageCount + 1)
                + "&&category=" + category + "\">→</a></li>");
        }

        out.println("</ul>");
        out.println("  </tbody>");
        out.println("</table>");
        out.println("<div class=\"write-post\">");
        out.println("  <a href=\"/board/write\">글쓰기</a>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");

        out.close();

    }

    private static void validateInputParameter(String category) {
        if (!category.equals("전체") && !category.equals("여행 경험 공유") && !category.equals("여행지 추천") && !category.equals(
            "여행 계획 토론")) {
            throw new InputValidationException("카테고리가 잘못입력되었습니다.");
        }
    }
}