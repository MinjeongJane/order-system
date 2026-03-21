Resolve a GitHub issue for the MinjeongJane/order-system repository.

The user will provide an issue number. Follow the steps below in order.

## Step 1. 이슈 불러오기
Fetch the issue details using:
```bash
gh issue view {issue_number} --repo MinjeongJane/order-system
```
Read the issue title, body, labels, and assignee. Summarize the issue to the user before proceeding.

## Step 2. 브랜치 세팅하기
Create and checkout a new branch named `issue-{issue_number}` from the latest main branch:
```bash
git fetch origin
git checkout -b issue-{issue_number} origin/main
```
Confirm the branch was created successfully.

## Step 3. 코드베이스 분석하기
Based on the issue content:
- Identify the relevant files mentioned in "수정 파일" section of the issue body
- Read those files and any related files to understand the current implementation
- Understand the root cause of the problem or the requirement for the new feature

## Step 4. 이슈 해결 계획 세우기
Present a clear plan to the user:
- What changes will be made and why
- Which files will be modified
- Any risks or trade-offs

Wait for user confirmation before proceeding to Step 5.

## Step 5. 이슈 해결하기
Implement the fix or feature according to the plan:
- Make minimal, focused changes
- Do not refactor unrelated code
- Follow existing code style and patterns in the project

## Step 6. 테스트 작성 및 검증하기
Delegate this step entirely to the `test-coverage-booster` subagent (defined in `.claude/agents/test-coverage-booster.md`).

Launch the subagent with the following context:
- Which files were modified in Step 5
- What the fix or feature does
- Ask the subagent to: write tests for the changed code, run them, fix any failures, and confirm all tests pass

Do not proceed to Step 8 until the subagent reports all tests are passing.

## Step 8. 풀 리퀘스트 생성하기
Create a pull request using:
```bash
gh pr create \
  --repo MinjeongJane/order-system \
  --base main \
  --head issue-{issue_number} \
  --title "{issue_title}" \
  --label "{issue_label}" \
  --assignee "MinjeongJane" \
  --body "$(cat <<'EOF'
## 개요
{issue 설명 요약}

## 변경 사항
{구현한 내용 요약}

## 수정 파일
{수정한 파일 목록}

## 테스트
- [ ] 단위 테스트 추가/수정
- [ ] 전체 테스트 통과 확인

## 연관 이슈
Closes #{issue_number}
EOF
)"
```

Return the PR URL to the user.