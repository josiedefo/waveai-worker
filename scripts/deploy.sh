#!/usr/bin/env bash
# Deploy waveai-worker to AWS Elastic Beanstalk (Java 21 / Corretto platform).
# Usage:
#   export WAVEAI_API_TOKEN=your-token
#   bash scripts/deploy.sh [region]          # default region: us-east-1
set -euo pipefail

APP_NAME="waveai-worker"
ENV_NAME="waveai-prod"
REGION="${1:-us-east-1}"
ZIP_FILE="target/waveai-worker-deploy.zip"

# ── Preflight checks ────────────────────────────────────────────────────────
if [ -z "${WAVEAI_API_TOKEN:-}" ]; then
  echo "ERROR: WAVEAI_API_TOKEN environment variable is not set."
  exit 1
fi

if ! command -v aws &>/dev/null; then
  echo "ERROR: AWS CLI is not installed."
  exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
BUCKET="${APP_NAME}-deploys-${ACCOUNT_ID}"
VERSION="v-$(date +%Y%m%d%H%M%S)"

echo "Account : ${ACCOUNT_ID}"
echo "Region  : ${REGION}"
echo "App     : ${APP_NAME}"
echo "Env     : ${ENV_NAME}"
echo ""

# ── Build ───────────────────────────────────────────────────────────────────
echo "▶ Building..."
mvn clean package -DskipTests -q
echo "  Built: ${ZIP_FILE}"

# ── S3 bucket ───────────────────────────────────────────────────────────────
echo "▶ Ensuring S3 bucket ${BUCKET}..."
if ! aws s3api head-bucket --bucket "${BUCKET}" --region "${REGION}" 2>/dev/null; then
  if [ "${REGION}" = "us-east-1" ]; then
    aws s3api create-bucket --bucket "${BUCKET}" --region "${REGION}"
  else
    aws s3api create-bucket --bucket "${BUCKET}" --region "${REGION}" \
      --create-bucket-configuration LocationConstraint="${REGION}"
  fi
  echo "  Created bucket ${BUCKET}"
fi

echo "▶ Uploading ${VERSION}.zip to s3://${BUCKET}..."
aws s3 cp "${ZIP_FILE}" "s3://${BUCKET}/${VERSION}.zip" --region "${REGION}"

# ── EB Application ──────────────────────────────────────────────────────────
echo "▶ Ensuring EB application ${APP_NAME}..."
aws elasticbeanstalk create-application \
  --application-name "${APP_NAME}" \
  --region "${REGION}" 2>/dev/null || true

# ── Application version ─────────────────────────────────────────────────────
echo "▶ Creating application version ${VERSION}..."
aws elasticbeanstalk create-application-version \
  --application-name "${APP_NAME}" \
  --version-label "${VERSION}" \
  --source-bundle "S3Bucket=${BUCKET},S3Key=${VERSION}.zip" \
  --region "${REGION}"

# ── Environment: create or update ──────────────────────────────────────────
ENV_STATUS=$(aws elasticbeanstalk describe-environments \
  --application-name "${APP_NAME}" \
  --environment-names "${ENV_NAME}" \
  --query "Environments[?Status!='Terminated'].Status" \
  --output text \
  --region "${REGION}")

if [ -z "${ENV_STATUS}" ]; then
  echo "▶ Creating environment ${ENV_NAME} (this takes ~5 minutes)..."

  STACK=$(aws elasticbeanstalk list-available-solution-stacks \
    --query "SolutionStacks[?contains(@, 'Corretto 21')] | [0]" \
    --output text \
    --region "${REGION}")
  echo "  Using solution stack: ${STACK}"

  aws elasticbeanstalk create-environment \
    --application-name "${APP_NAME}" \
    --environment-name "${ENV_NAME}" \
    --solution-stack-name "${STACK}" \
    --version-label "${VERSION}" \
    --option-settings \
      "Namespace=aws:elasticbeanstalk:application:environment,OptionName=WAVEAI_API_TOKEN,Value=${WAVEAI_API_TOKEN}" \
    --region "${REGION}"
else
  echo "▶ Deploying to existing environment ${ENV_NAME}..."
  aws elasticbeanstalk update-environment \
    --application-name "${APP_NAME}" \
    --environment-name "${ENV_NAME}" \
    --version-label "${VERSION}" \
    --region "${REGION}"
fi

# ── Wait for deployment to finish ───────────────────────────────────────────
echo "▶ Waiting for deployment to complete..."
aws elasticbeanstalk wait environment-updated \
  --application-name "${APP_NAME}" \
  --environment-names "${ENV_NAME}" \
  --region "${REGION}"

# ── Print result ────────────────────────────────────────────────────────────
CNAME=$(aws elasticbeanstalk describe-environments \
  --application-name "${APP_NAME}" \
  --environment-names "${ENV_NAME}" \
  --query "Environments[0].CNAME" \
  --output text \
  --region "${REGION}")

HEALTH=$(aws elasticbeanstalk describe-environments \
  --application-name "${APP_NAME}" \
  --environment-names "${ENV_NAME}" \
  --query "Environments[0].HealthStatus" \
  --output text \
  --region "${REGION}")

echo ""
echo "✓ Deployment complete!"
echo "  URL    : http://${CNAME}/waveai/"
echo "  Health : ${HEALTH}"
