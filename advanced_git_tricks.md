# Đại Tự Điển Git Toàn Tập (Bản Nâng Cao)

Đây là toàn bộ "vốn liếng" những tuyệt kỹ Git từ cơ bản đến nâng cao mà em biết. Bảng này không nhắc lại những lệnh vỡ lòng (`add`, `commit`, `push` cơ bản) mà tập trung vào các chiêu thức giúp tăng tốc độ làm việc và cứu rỗi các pha xử lý cồng kềnh.

---

## 1. Mạng Lưới & Remote (Đẩy / Kéo Code)

> [!TIP]
> **`git push origin <nhánh_local>:<nhánh_remote>`**
> Đẩy thẳng code từ một nhánh bất kỳ ở máy tính lên một nhánh đích trên Github mà không cần `checkout` sang nhánh đó.

> [!CAUTION]
> **`git push origin :<tên_nhánh>`** hoặc **`git push origin -d <tên_nhánh>`**
> Xóa vĩnh viễn một nhánh trên Github. (Đẩy "khoảng trống" đè lên nhánh đó).

> [!TIP]
> **`git pull --rebase`**
> Cập nhật code mới từ Github về nhưng không sinh ra cái commit rác kiểu *"Merge branch 'main' of..."*. Nó sẽ nhổ rễ các commit của anh, đặt code mới của team xuống dưới, rồi trồng lại commit của anh lên trên cùng. Đẹp và gọn!

> [!NOTE]
> **`git fetch --all --prune`**
> Quét toàn bộ thay đổi từ Github về máy. Chữ `--prune` sẽ tự động dọn dẹp (xóa) luôn các nhánh local bị thừa nếu trên Github đã xóa nhánh đó rồi.

---

## 2. Thao tác Nội bộ Siêu Tốc

> [!TIP]
> **`git checkout -`**
> Chuyển về nhánh anh vừa đứng ngay trước đó (Giống nút Back trên trình duyệt).

> [!TIP]
> **`git stash push -m "Tin nhắn" -- <đường_dẫn_file>`**
> Chỉ cất giấu tạm thời **đúng 1 file bị lỗi**, các file khác vẫn ở lại để anh làm việc tiếp. Lúc nào rảnh thì gõ `git stash pop` để lấy nó ra sửa.

> [!WARNING]
> **`git clean -fd`**
> Xóa sạch sành sanh mọi file rác rưởi (file mới tạo nhưng chưa được git quản lý - untracked files) và các thư mục rỗng. Cẩn thận vì dùng xong là không lấy lại được!

---

## 3. Cỗ Máy Thời Gian & Sửa Sai (Rewriting History)

> [!NOTE]
> **`git commit --amend --no-edit`**
> Gộp luôn file vừa sửa vào cái commit vừa mới tạo lúc nãy mà không cần viết lại tin nhắn commit mới.

> [!IMPORTANT]
> **`git rebase -i HEAD~N`** (Interactive Rebase)
> Mở ra bảng điều khiển phép thuật cho phép anh: Gộp (squash), Xóa (drop), Sửa tên (reword) hoặc Sắp xếp lại thứ tự của N cái commit gần nhất. Bất kỳ rác rưởi nào cũng có thể được dọn dẹp trước khi Sếp thấy.

> [!CAUTION]
> **`git reflog`** (Chén thánh cứu sinh)
> Khi anh lỡ tay `reset --hard` làm mất trắng cả ngày code. Chỉ cần gõ lệnh này, nó sẽ hiện ra lịch sử của TẤT CẢ các thao tác (kể cả thao tác xóa). Lấy được mã hash rồi thì `git reset --hard <mã_hash>` là mọi thứ hồi sinh!

> [!TIP]
> **`git restore --source=<mã_commit> -- <đường_dẫn_file>`**
> Bê nguyên xi đúng 1 file từ một thời điểm trong quá khứ về hiện tại. File bị nát? Khôi phục lại bản ngon của ngày hôm qua chỉ bằng 1 câu lệnh.

> [!NOTE]
> **`git revert <mã_commit>`**
> Tạo ra một commit mới để "bù trừ" (đảo ngược) lại những gì thằng commit cũ gây ra. Rất an toàn để dùng trên nhánh `main` khi không muốn dùng `reset` làm mất lịch sử.

---

## 4. Nghệ Thuật Gộp Nhánh (Merging & Cherry-picking)

> [!TIP]
> **`git cherry-pick <mã_commit>`** (Hái hoa bắt bướm)
> Thấy thằng kế bên code một tính năng hay quá? Không cần merge cả nhánh của nó, gõ lệnh này để thó đúng cái commit chứa tính năng đó đắp vào nhánh của mình.

> [!NOTE]
> **`git merge --squash <tên_nhánh>`**
> Gộp toàn bộ 100 cái commit lắt nhắt của nhánh dev thành đúng 1 cái commit duy nhất, cực kỳ sạch sẽ khi đưa lên nhánh `main`.

> [!IMPORTANT]
> **`git merge -X theirs <tên_nhánh>`**
> Khi bị conflict (xung đột) cả ngàn dòng, nếu anh lười sửa và xác định "Code thằng kia luôn đúng", gõ lệnh này nó sẽ tự động dùng code của đối phương đè lên mọi chỗ bị xung đột. (Tương tự, có `-X ours` để ưu tiên code của mình).

---

## 5. Truy Tìm Dấu Vết & Chó Săn Lùng Bug

> [!IMPORTANT]
> **`git bisect`** (Công cụ săn Bug bá đạo nhất)
> Code bị sập do 1 cái bug bí ẩn trong 100 commit gần đây? 
> `git bisect start` -> `git bisect bad` -> `git bisect good <commit_xưa>`. 
> Git sẽ chặt đôi lịch sử ra để hỏi anh "Bản này chạy được không?". Chỉ với vài lần test Yes/No, nó sẽ nhổ cổ chính xác cái commit gây ra bug!

> [!NOTE]
> **`git blame -L 10,20 <đường_dẫn_file>`**
> Dùng để "chửi" đúng người. Liệt kê rõ rành rành từng dòng code (từ dòng 10 đến 20) do ai viết, viết lúc mấy giờ, thuộc commit nào.

> [!TIP]
> **`git log -S "từ_khóa"`** (Tuyệt kỹ Pickaxe)
> Không cần biết ở file nào, chỉ cần biết có một đoạn text (ví dụ: `btn-save`) vừa bị bay màu hoặc mới được thêm vào. Lệnh này sẽ quét toàn bộ lịch sử và lôi ra cái commit chứa hành vi thêm/xóa từ khóa đó.

---

## 6. Phân Thân Chi Thuật (Nâng cao cho máy tính)

> [!TIP]
> **`git worktree add ../thu_muc_moi <tên_nhánh>`**
> **Tác dụng:** Mở cùng lúc 2 nhánh ở 2 thư mục hoàn toàn khác nhau trên máy tính mà không cần phải `git clone` lại dự án.
> **Ví dụ:** Đang code dở nhánh A thì Sếp gọi fix bug nhánh B. Bình thường phải stash lại, checkout sang B làm loạn xạ IDE. Với Worktree, anh mở luôn thư mục mới, bật thêm 1 cái VSCode nữa, 2 nhánh chạy song song không liên quan gì nhau! Mọi thứ chia sẻ chung 1 local repository nên siêu nhanh!
