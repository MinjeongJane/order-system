Create a GitHub issue for the MinjeongJane/order-system repository.

Ask the user for the following information:
- title: Issue title
- label: One of "documentation", "기능", "테스트", "개선", "오류수정"
- 설명: Description of the issue
- 작업자: GitHub username of the assignee (default: MinjeongJane)
- 구현 참고사항: Implementation notes (optional)
- 수정 파일: Files to be modified (optional)
- 기술적 고려사항: Technical considerations (optional)
- 의존성: Dependencies (optional)

Then create the issue using the gh CLI with the following body format:

```
## issue template
- title : {title}
- label : {label}
- 설명 : {설명}
- 작업자 : {작업자}

## 구현 참고사항
{구현 참고사항}

## 수정 파일
{수정 파일}

## 기술적 고려사항
{기술적 고려사항}

## 의존성
{의존성}
```

Run the following command to create the issue:
```bash
gh issue create \
  --repo MinjeongJane/order-system \
  --title "{title}" \
  --body "{body}" \
  --label "{label}" \
  --assignee "{작업자}"
```

After creating the issue, show the user the issue URL.